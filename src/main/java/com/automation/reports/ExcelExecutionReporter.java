package com.automation.reports;

import com.automation.config.ExcelExecutionConfig;
import com.automation.models.ExecutionResult;
import com.automation.models.Scenario;
import com.automation.models.TestCaseBlock;
import com.automation.services.ScreenshotService;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExcelExecutionReporter {

    private static final Logger LOGGER = LogManager.getLogger(ExcelExecutionReporter.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] STEP_TABLE_HEADERS = {
            "Step",
            "Excel Row",
            "Description",
            "Keyword",
            "Object",
            "Application",
            "Raw Value",
            "Resolved Value",
            "Raw XPath",
            "Resolved XPath",
            "Executed By",
            "Status",
            "Evidence"
    };
    private static ExtentReports sharedExtentReports;
    private static Path sharedReportFilePath;
    private static Path latestReportFilePath;

    private record EvidenceItem(String label, String path) {
    }

    private final WebDriver driver;
    private final ExcelReportConfig config;
    private final ExcelExecutionConfig executionConfig;
    private final ScreenshotService screenshotService;
    private final SensitiveDataMasker sensitiveDataMasker;
    private final ExtentReports extentReports;

    private ExtentTest scenarioNode;
    private ExtentTest testCaseNode;
    private Instant scenarioStartTime;
    private Instant testCaseStartTime;
    private final List<String[]> currentStepRows = new ArrayList<>();
    private final List<EvidenceItem> currentEvidenceItems = new ArrayList<>();
    private ExecutionResult currentFailureResult;
    private String currentFailureEvidence;

    public ExcelExecutionReporter(WebDriver driver) {
        this(driver, ExcelReportConfig.fromConfig());
    }

    public ExcelExecutionReporter(WebDriver driver, ExcelReportConfig config) {
        this(driver, config, ExcelExecutionConfig.load());
    }

    public ExcelExecutionReporter(WebDriver driver, ExcelReportConfig config, ExcelExecutionConfig executionConfig) {
        this(driver, config, executionConfig, new SensitiveDataMasker());
    }

    public ExcelExecutionReporter(WebDriver driver, ExcelReportConfig config, SensitiveDataMasker sensitiveDataMasker) {
        this(driver, config, ExcelExecutionConfig.load(), sensitiveDataMasker);
    }

    public ExcelExecutionReporter(
            WebDriver driver,
            ExcelReportConfig config,
            ExcelExecutionConfig executionConfig,
            SensitiveDataMasker sensitiveDataMasker
    ) {
        this.driver = driver;
        this.config = config == null ? ExcelReportConfig.fromConfig() : config;
        this.executionConfig = executionConfig == null ? ExcelExecutionConfig.load() : executionConfig;
        this.executionConfig.validate();
        this.screenshotService = new ScreenshotService(this.executionConfig.getScreenshotOutputDirectory());
        this.sensitiveDataMasker = sensitiveDataMasker == null ? new SensitiveDataMasker() : sensitiveDataMasker;
        this.extentReports = getExcelReport(this.executionConfig);
    }

    public void startScenario(Scenario scenario) {
        scenarioStartTime = Instant.now();
        String scenarioName = "Scenario: [" + safe(scenario.getNo()) + "] " + safe(scenario.getScenarioName());
        scenarioNode = createScenarioNode(scenarioName);
        LOGGER.info("Excel report scenario node started: {}", scenarioName);
    }

    public void finishScenario(Scenario scenario, boolean success, String message) {
        Instant endTime = Instant.now();
        String status = success ? ExecutionResult.STATUS_PASS : ExecutionResult.STATUS_FAIL;
        if (scenarioNode != null) {
            if (success) {
                scenarioNode.pass(scenarioSummaryHtml(scenario, status, scenarioStartTime, endTime, message));
            } else {
                scenarioNode.fail(scenarioSummaryHtml(scenario, status, scenarioStartTime, endTime, message));
            }
        }
    }

    public void startTestCase(TestCaseBlock testCaseBlock) {
        testCaseStartTime = Instant.now();
        currentStepRows.clear();
        currentEvidenceItems.clear();
        currentFailureResult = null;
        currentFailureEvidence = "";
        if (scenarioNode == null) {
            LOGGER.warn("Scenario node was not initialized before testcase reporting.");
            return;
        }
        testCaseNode = scenarioNode.createNode("Testcase: " + safe(testCaseBlock.getTestcaseName()));
        LOGGER.info("Excel report testcase node started: {}", testCaseBlock.getTestcaseName());
    }

    public void finishTestCase(TestCaseBlock testCaseBlock, boolean success, String message) {
        Instant endTime = Instant.now();
        String status = success ? ExecutionResult.STATUS_PASS : ExecutionResult.STATUS_FAIL;
        if (testCaseNode == null) {
            return;
        }
        if (success) {
            testCaseNode.pass(testcaseSummaryHtml(testCaseBlock, status, testCaseStartTime, endTime, message));
        } else {
            testCaseNode.fail(testcaseSummaryHtml(testCaseBlock, status, testCaseStartTime, endTime, message));
        }
        testCaseNode.info(stepTableHtml(currentStepRows));
        if (!currentEvidenceItems.isEmpty()) {
            testCaseNode.info(evidenceGalleryHtml(currentEvidenceItems));
        }
        if (!success && currentFailureResult != null) {
            testCaseNode.info(failureDetailHtml(currentFailureResult, currentFailureEvidence));
        }
    }

    public void logStep(ExecutionResult result) {
        if (testCaseNode == null) {
            return;
        }

        String evidence = evidenceFor(result);
        currentStepRows.add(stepRow(result, evidence));

        if (ExecutionResult.STATUS_SKIP.equals(result.getStatus())) {
            testCaseNode.skip("Step " + result.getStepOrder() + " skipped: " + safe(result.getMessage()));
        } else if (!result.isSuccess()) {
            currentFailureResult = result;
            currentFailureEvidence = evidence;
        }
    }

    public void attachScreenshot(ExecutionResult result, String screenshotPath, String label) {
        if (testCaseNode == null || screenshotPath == null || screenshotPath.isBlank()) {
            return;
        }
        currentEvidenceItems.add(new EvidenceItem(safe(label), screenshotPath));
    }

    public void flush() {
        synchronized (ExcelExecutionReporter.class) {
            extentReports.flush();
        }
    }

    public static String getReportFilePath() {
        synchronized (ExcelExecutionReporter.class) {
            if (latestReportFilePath != null) {
                return latestReportFilePath.toString();
            }
        }
        return ExcelExecutionConfig.load().getReportFilePath().toString();
    }

    private ExtentTest createScenarioNode(String scenarioName) {
        synchronized (ExcelExecutionReporter.class) {
            return extentReports.createTest(scenarioName, "Excel-driven scenario execution");
        }
    }

    private static synchronized ExtentReports getExcelReport(ExcelExecutionConfig executionConfig) {
        Path reportFilePath = executionConfig.getReportFilePath();
        if (sharedExtentReports == null || sharedReportFilePath == null || !sharedReportFilePath.equals(reportFilePath)) {
            sharedExtentReports = createExcelReport(executionConfig);
            sharedReportFilePath = reportFilePath;
        }
        latestReportFilePath = reportFilePath;
        return sharedExtentReports;
    }

    private static ExtentReports createExcelReport(ExcelExecutionConfig executionConfig) {
        try {
            Files.createDirectories(executionConfig.getReportOutputDirectory());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create Excel execution report directory.", exception);
        }

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(executionConfig.getReportFilePath().toString());
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setDocumentTitle("Excel Automation Report");
        sparkReporter.config().setReportName("Excel-Driven Automation Execution");

        ExtentReports reports = new ExtentReports();
        reports.attachReporter(sparkReporter);
        reports.setSystemInfo("Report Type", "Excel Keyword Execution");
        reports.setSystemInfo("Framework", "Selenium Java TestNG");
        reports.setSystemInfo("Java Version", System.getProperty("java.version"));
        reports.setSystemInfo("Operating System", System.getProperty("os.name"));
        reports.setSystemInfo("Scenario File", executionConfig.getScenarioFilePath().toString());
        reports.setSystemInfo("Report File", executionConfig.getReportFilePath().toString());
        reports.setSystemInfo("Screenshot Directory", executionConfig.getScreenshotOutputDirectory().toString());
        return reports;
    }

    private String evidenceFor(ExecutionResult result) {
        if (!result.isSuccess() && config.isScreenshotOnFailure()) {
            String screenshotPath = captureFailureScreenshot(result);
            if (!screenshotPath.isBlank()) {
                attachScreenshot(result, screenshotPath, "Failure screenshot");
                return screenshotLink(screenshotPath, "Failure screenshot");
            }
        }

        String evidence = safe(result.getEvidence());
        if (evidence.toLowerCase(Locale.ROOT).endsWith(".png")) {
            attachScreenshot(result, evidence, screenshotLabel(result));
            return screenshotLink(evidence, screenshotLabel(result));
        }
        return evidence;
    }

    private String captureFailureScreenshot(ExecutionResult result) {
        if (driver == null) {
            return "";
        }
        String screenshotName = screenshotBaseName(result, "Failure");
        String screenshotPath = screenshotService.capture(driver, screenshotName);
        return screenshotPath == null ? "" : screenshotPath;
    }

    private String[] stepRow(ExecutionResult result, String evidence) {
        return new String[]{
                String.valueOf(result.getStepOrder()),
                String.valueOf(result.getExcelRowNumber()),
                safe(result.getDescription()),
                safe(result.getKeywordName()),
                safe(result.getObjectName()),
                safe(result.getApplication()),
                displayRawValue(result),
                displayResolvedValue(result),
                safe(result.getRawXPath()),
                safe(result.getResolvedXPath()),
                executedBy(result),
                safe(result.getStatus()),
                safe(evidence)
        };
    }

    private String maskedRawValue(ExecutionResult result) {
        return sensitiveDataMasker.maskRawValueIfNeeded(
                safe(result.getRawValue()),
                config.isShowSensitiveData(),
                result.getObjectName(),
                result.getResolvedXPath(),
                result.getDescription(),
                result.getKeywordName()
        );
    }

    private String displayRawValue(ExecutionResult result) {
        return shortenLocalFileUri(maskedRawValue(result));
    }

    private String maskedResolvedValue(ExecutionResult result) {
        return sensitiveDataMasker.maskIfNeeded(
                safe(result.getResolvedValue()),
                config.isShowSensitiveData(),
                result.getRawValue(),
                result.getObjectName(),
                result.getResolvedXPath(),
                result.getDescription(),
                result.getKeywordName()
        );
    }

    private String displayResolvedValue(ExecutionResult result) {
        return shortenLocalFileUri(maskedResolvedValue(result));
    }

    private String shortenLocalFileUri(String value) {
        String safeValue = safe(value);
        if (!safeValue.toLowerCase(Locale.ROOT).startsWith("file:/")) {
            return safeValue;
        }

        int lastSlashIndex = Math.max(safeValue.lastIndexOf('/'), safeValue.lastIndexOf('\\'));
        if (lastSlashIndex < 0 || lastSlashIndex >= safeValue.length() - 1) {
            return "file:///...";
        }
        return "file:///.../" + safeValue.substring(lastSlashIndex + 1);
    }

    private String executedBy(ExecutionResult result) {
        String executedByClass = safe(result.getExecutedByClass());
        if (executedByClass.isBlank()) {
            return "";
        }
        String simpleName = executedByClass.substring(executedByClass.lastIndexOf('.') + 1);
        if (safe(result.getKeywordName()).isBlank()) {
            return simpleName;
        }
        return simpleName + "." + result.getKeywordName();
    }

    private String stepTableHtml(List<String[]> rows) {
        StringBuilder html = new StringBuilder();
        html.append("<h4>Steps</h4>");
        html.append("<table class='excel-step-table' style='border-collapse:collapse;width:100%;font-size:12px;'>");
        html.append("<thead><tr>");
        for (String header : STEP_TABLE_HEADERS) {
            html.append("<th style='border:1px solid #d0d7de;padding:6px;background:#f6f8fa;text-align:left;'>")
                    .append(escape(header))
                    .append("</th>");
        }
        html.append("</tr></thead><tbody>");
        for (String[] row : rows) {
            html.append("<tr>");
            for (String cell : row) {
                html.append("<td style='border:1px solid #d0d7de;padding:6px;vertical-align:top;'>")
                        .append(cell.startsWith("<a ") ? cell : escape(cell))
                        .append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String scenarioSummaryHtml(Scenario scenario, String status, Instant startTime, Instant endTime, String message) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Scenario NO", safe(scenario.getNo())});
        rows.add(new String[]{"Scenario Name", safe(scenario.getScenarioName())});
        rows.add(new String[]{"Scenario ACTION", safe(scenario.getAction())});
        rows.add(new String[]{"Status", status});
        rows.add(new String[]{"Start Time", formatTime(startTime)});
        rows.add(new String[]{"End Time", formatTime(endTime)});
        rows.add(new String[]{"Duration", formatDuration(startTime, endTime)});
        String failureMessage = cleanFailureMessage(message);
        if (!ExecutionResult.STATUS_PASS.equals(status) && !failureMessage.isBlank()) {
            rows.add(new String[]{"Failure Summary", failureMessage});
        }
        return sectionHtml("Summary", summaryTableHtml(rows));
    }

    private String testcaseSummaryHtml(TestCaseBlock testCaseBlock, String status, Instant startTime, Instant endTime, String message) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Testcase", safe(testCaseBlock.getTestcaseName())});
        rows.add(new String[]{"Application", safe(testCaseBlock.getApplication())});
        rows.add(new String[]{"Parent Excel Row", String.valueOf(testCaseBlock.getExcelRowNumber())});
        rows.add(new String[]{"Step Count", String.valueOf(testCaseBlock.getSteps().size())});
        rows.add(new String[]{"Status", status});
        rows.add(new String[]{"Duration", formatDuration(startTime, endTime)});
        String safeMessage = safe(message);
        if (!ExecutionResult.STATUS_PASS.equals(status) && !safeMessage.isBlank()) {
            rows.add(new String[]{"Message", safeMessage});
        }
        return sectionHtml("Summary", summaryTableHtml(rows));
    }

    private String sectionHtml(String heading, String bodyHtml) {
        return "<section><h4>" + escape(heading) + "</h4>" + bodyHtml + "</section>";
    }

    private String summaryTableHtml(List<String[]> rows) {
        StringBuilder html = new StringBuilder();
        html.append("<table style='border-collapse:collapse;width:70%;font-size:12px;'>");
        for (String[] row : rows) {
            html.append("<tr><th style='border:1px solid #d0d7de;padding:5px;background:#f6f8fa;text-align:left;width:180px;'>")
                    .append(escape(row[0]))
                    .append("</th><td style='border:1px solid #d0d7de;padding:5px;'>")
                    .append(row[1].startsWith("<a ") ? row[1] : escape(row[1]))
                    .append("</td></tr>");
        }
        html.append("</table>");
        return html.toString();
    }

    private String failureDetailHtml(ExecutionResult result, String evidence) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='excel-failure-detail'>");
        html.append("<h4>Failure Details</h4>");
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Scenario NO", safe(result.getScenarioNo())});
        rows.add(new String[]{"Scenario ACTION", safe(result.getScenarioAction())});
        rows.add(new String[]{"Testcase", safe(result.getTestcaseName())});
        rows.add(new String[]{"Excel Row", String.valueOf(result.getExcelRowNumber())});
        rows.add(new String[]{"Keyword", safe(result.getKeywordName())});
        rows.add(new String[]{"Object", safe(result.getObjectName())});
        rows.add(new String[]{"Application", safe(result.getApplication())});
        rows.add(new String[]{"Error Message", safe(result.getMessage())});
        if (!safe(evidence).isBlank()) {
            rows.add(new String[]{"Screenshot", evidence});
        }
        html.append(summaryTableHtml(rows));
        html.append("</div>");
        return html.toString();
    }

    private String evidenceGalleryHtml(List<EvidenceItem> evidenceItems) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='excel-evidence-gallery'>");
        html.append("<h4>Evidence Gallery</h4>");
        html.append("<div style='display:flex;flex-wrap:wrap;gap:12px;'>");
        for (EvidenceItem item : evidenceItems) {
            String relativePath = toReportRelativePath(item.path());
            String label = safe(item.label());
            html.append("<figure style='margin:0;width:220px;'>");
            html.append("<a href='")
                    .append(escapeAttribute(relativePath))
                    .append("' target='_blank'>")
                    .append("<img src='")
                    .append(escapeAttribute(relativePath))
                    .append("' alt='")
                    .append(escapeAttribute(label))
                    .append("' style='width:220px;max-height:140px;object-fit:contain;border:1px solid #d0d7de;'>")
                    .append("</a>");
            html.append("<figcaption style='font-size:12px;margin-top:4px;'>")
                    .append(escape(label))
                    .append("</figcaption>");
            html.append("</figure>");
        }
        html.append("</div></div>");
        return html.toString();
    }

    private String screenshotLink(String screenshotPath, String label) {
        String relativePath = toReportRelativePath(screenshotPath);
        return "<a href='" + escapeAttribute(relativePath) + "' target='_blank'>" + escape(label) + "</a>";
    }

    private String toReportRelativePath(String screenshotPath) {
        try {
            Path reportDir = executionConfig.getReportOutputDirectory().toAbsolutePath();
            Path screenshot = Path.of(screenshotPath).toAbsolutePath();
            return reportDir.relativize(screenshot).toString().replace('\\', '/');
        } catch (RuntimeException exception) {
            return screenshotPath.replace('\\', '/');
        }
    }

    private String screenshotLabel(ExecutionResult result) {
        if ("screenshot".equalsIgnoreCase(safe(result.getKeywordName()).trim())) {
            String label = safe(result.getResolvedValue());
            return label.isBlank() ? "Manual screenshot" : "Manual screenshot: " + label;
        }
        return safe(result.getResolvedValue()).isBlank()
                ? "Screenshot"
                : result.getResolvedValue();
    }

    private String screenshotBaseName(ExecutionResult result, String label) {
        return String.join(
                "_",
                safe(result.getScenarioNo()),
                safe(result.getTestcaseName()),
                "step" + result.getStepOrder(),
                "row" + result.getExcelRowNumber(),
                safe(label)
        );
    }

    private String formatTime(Instant time) {
        return time == null ? "" : LocalDateTime.ofInstant(time, ZoneId.systemDefault()).format(TIME_FORMAT);
    }

    private String formatDuration(Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null) {
            return "";
        }
        long millis = Duration.between(startTime, endTime).toMillis();
        return millis + " ms";
    }

    private String cleanFailureMessage(String message) {
        String cleanedMessage = safe(message).trim();
        if (cleanedMessage.isBlank()) {
            return "";
        }

        String duplicatedPrefix = "Scenario failed. Scenario failed.";
        while (cleanedMessage.startsWith(duplicatedPrefix)) {
            cleanedMessage = "Scenario failed." + cleanedMessage.substring(duplicatedPrefix.length());
        }

        if (cleanedMessage.startsWith("Scenario failed.")) {
            int errorStartIndex = cleanedMessage.indexOf(". Failed");
            if (errorStartIndex >= 0 && errorStartIndex < cleanedMessage.length() - 2) {
                return cleanedMessage.substring(errorStartIndex + 2);
            }
        }
        return cleanedMessage;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String escape(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeAttribute(String value) {
        return escape(value).replace(" ", "%20");
    }
}
