package com.automation.reports;

import com.automation.constants.FrameworkConstants;
import com.automation.models.ExecutionResult;
import com.automation.models.Scenario;
import com.automation.models.TestCaseBlock;
import com.automation.utils.ScreenshotUtil;
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
            "Function",
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

    private final WebDriver driver;
    private final ExcelReportConfig config;
    private final SensitiveDataMasker sensitiveDataMasker;
    private final ExtentReports extentReports;

    private ExtentTest scenarioNode;
    private ExtentTest testCaseNode;
    private Instant scenarioStartTime;
    private Instant testCaseStartTime;
    private final List<String[]> currentStepRows = new ArrayList<>();

    public ExcelExecutionReporter(WebDriver driver) {
        this(driver, ExcelReportConfig.fromConfig());
    }

    public ExcelExecutionReporter(WebDriver driver, ExcelReportConfig config) {
        this(driver, config, new SensitiveDataMasker());
    }

    public ExcelExecutionReporter(WebDriver driver, ExcelReportConfig config, SensitiveDataMasker sensitiveDataMasker) {
        this.driver = driver;
        this.config = config == null ? ExcelReportConfig.fromConfig() : config;
        this.sensitiveDataMasker = sensitiveDataMasker == null ? new SensitiveDataMasker() : sensitiveDataMasker;
        this.extentReports = getExcelReport();
    }

    public void startScenario(Scenario scenario) {
        scenarioStartTime = Instant.now();
        String scenarioName = "Scenario: [" + safe(scenario.getNo()) + "] " + safe(scenario.getScenarioName());
        scenarioNode = createScenarioNode(scenarioName);
        scenarioNode.info(scenarioSummaryHtml(scenario, "RUNNING", scenarioStartTime, null, ""));
        LOGGER.info("Excel report scenario node started: {}", scenarioName);
    }

    public void finishScenario(Scenario scenario, boolean success, String message) {
        Instant endTime = Instant.now();
        String status = success ? ExecutionResult.STATUS_PASS : ExecutionResult.STATUS_FAIL;
        if (scenarioNode != null) {
            scenarioNode.info(scenarioSummaryHtml(scenario, status, scenarioStartTime, endTime, message));
            if (success) {
                scenarioNode.pass("Scenario finished successfully.");
            } else {
                scenarioNode.fail("Scenario failed. " + safe(message));
            }
        }
    }

    public void startTestCase(TestCaseBlock testCaseBlock) {
        testCaseStartTime = Instant.now();
        currentStepRows.clear();
        if (scenarioNode == null) {
            LOGGER.warn("Scenario node was not initialized before testcase reporting.");
            return;
        }
        testCaseNode = scenarioNode.createNode("Testcase: " + safe(testCaseBlock.getTestcaseName()));
        testCaseNode.info(testcaseSummaryHtml(testCaseBlock, "RUNNING", testCaseStartTime, null, ""));
        LOGGER.info("Excel report testcase node started: {}", testCaseBlock.getTestcaseName());
    }

    public void finishTestCase(TestCaseBlock testCaseBlock, boolean success, String message) {
        Instant endTime = Instant.now();
        String status = success ? ExecutionResult.STATUS_PASS : ExecutionResult.STATUS_FAIL;
        if (testCaseNode == null) {
            return;
        }
        testCaseNode.info(stepTableHtml(currentStepRows));
        testCaseNode.info(testcaseSummaryHtml(testCaseBlock, status, testCaseStartTime, endTime, message));
        if (success) {
            testCaseNode.pass("Testcase finished successfully.");
        } else {
            testCaseNode.fail("Testcase failed. " + safe(message));
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
        } else if (result.isSuccess()) {
            testCaseNode.pass("Step " + result.getStepOrder() + " passed: " + result.getFunctionName());
        } else {
            testCaseNode.fail("Step " + result.getStepOrder() + " failed: " + safe(result.getMessage()));
            testCaseNode.info(failureDetailHtml(result, evidence));
        }
    }

    public void attachScreenshot(ExecutionResult result, String screenshotPath, String label) {
        if (testCaseNode == null || screenshotPath == null || screenshotPath.isBlank()) {
            return;
        }

        try {
            testCaseNode.addScreenCaptureFromPath(toReportRelativePath(screenshotPath), safe(label));
        } catch (Exception exception) {
            LOGGER.warn("Could not attach screenshot to Excel execution report: {}", screenshotPath, exception);
        }
    }

    public void flush() {
        synchronized (ExcelExecutionReporter.class) {
            extentReports.flush();
        }
    }

    public static String getReportFilePath() {
        return FrameworkConstants.EXCEL_REPORT_FILE;
    }

    private ExtentTest createScenarioNode(String scenarioName) {
        synchronized (ExcelExecutionReporter.class) {
            return extentReports.createTest(scenarioName, "Excel-driven scenario execution");
        }
    }

    private static synchronized ExtentReports getExcelReport() {
        if (sharedExtentReports == null) {
            sharedExtentReports = createExcelReport();
        }
        return sharedExtentReports;
    }

    private static ExtentReports createExcelReport() {
        try {
            Files.createDirectories(Path.of(FrameworkConstants.EXCEL_REPORT_DIR));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create Excel execution report directory.", exception);
        }

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(FrameworkConstants.EXCEL_REPORT_FILE);
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setDocumentTitle("Excel Automation Report");
        sparkReporter.config().setReportName("Excel-Driven Automation Execution");

        ExtentReports reports = new ExtentReports();
        reports.attachReporter(sparkReporter);
        reports.setSystemInfo("Report Type", "Excel Keyword Execution");
        reports.setSystemInfo("Framework", "Selenium Java TestNG");
        reports.setSystemInfo("Java Version", System.getProperty("java.version"));
        reports.setSystemInfo("Operating System", System.getProperty("os.name"));
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
        String screenshotPath = ScreenshotUtil.captureScreenshot(driver, screenshotName);
        return screenshotPath == null ? "" : screenshotPath;
    }

    private String[] stepRow(ExecutionResult result, String evidence) {
        return new String[]{
                String.valueOf(result.getStepOrder()),
                String.valueOf(result.getExcelRowNumber()),
                safe(result.getDescription()),
                safe(result.getFunctionName()),
                safe(result.getObjectName()),
                safe(result.getApplication()),
                maskedRawValue(result),
                maskedResolvedValue(result),
                safe(result.getRawXpath()),
                safe(result.getResolvedXpath()),
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
                result.getResolvedXpath(),
                result.getDescription(),
                result.getFunctionName()
        );
    }

    private String maskedResolvedValue(ExecutionResult result) {
        return sensitiveDataMasker.maskIfNeeded(
                safe(result.getResolvedValue()),
                config.isShowSensitiveData(),
                result.getRawValue(),
                result.getObjectName(),
                result.getResolvedXpath(),
                result.getDescription(),
                result.getFunctionName()
        );
    }

    private String executedBy(ExecutionResult result) {
        String executedByClass = safe(result.getExecutedByClass());
        if (executedByClass.isBlank()) {
            return "";
        }
        String simpleName = executedByClass.substring(executedByClass.lastIndexOf('.') + 1);
        if (safe(result.getFunctionName()).isBlank()) {
            return simpleName;
        }
        return simpleName + "." + result.getFunctionName();
    }

    private String stepTableHtml(List<String[]> rows) {
        StringBuilder html = new StringBuilder();
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
        return summaryTableHtml(new String[][]{
                {"Scenario NO", safe(scenario.getNo())},
                {"Scenario Name", safe(scenario.getScenarioName())},
                {"Scenario ACTION", safe(scenario.getAction())},
                {"Status", status},
                {"Start Time", formatTime(startTime)},
                {"End Time", formatTime(endTime)},
                {"Duration", formatDuration(startTime, endTime)},
                {"Failure Summary", safe(message)}
        });
    }

    private String testcaseSummaryHtml(TestCaseBlock testCaseBlock, String status, Instant startTime, Instant endTime, String message) {
        return summaryTableHtml(new String[][]{
                {"Testcase", safe(testCaseBlock.getTestcaseName())},
                {"Application", safe(testCaseBlock.getApplication())},
                {"Parent Excel Row", String.valueOf(testCaseBlock.getExcelRowNumber())},
                {"Step Count", String.valueOf(testCaseBlock.getSteps().size())},
                {"Status", status},
                {"Start Time", formatTime(startTime)},
                {"End Time", formatTime(endTime)},
                {"Duration", formatDuration(startTime, endTime)},
                {"Message", safe(message)}
        });
    }

    private String summaryTableHtml(String[][] rows) {
        StringBuilder html = new StringBuilder();
        html.append("<table style='border-collapse:collapse;width:70%;font-size:12px;'>");
        for (String[] row : rows) {
            html.append("<tr><th style='border:1px solid #d0d7de;padding:5px;background:#f6f8fa;text-align:left;width:180px;'>")
                    .append(escape(row[0]))
                    .append("</th><td style='border:1px solid #d0d7de;padding:5px;'>")
                    .append(escape(row[1]))
                    .append("</td></tr>");
        }
        html.append("</table>");
        return html.toString();
    }

    private String failureDetailHtml(ExecutionResult result, String evidence) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='excel-failure-detail'>");
        html.append("<h4>Failure Detail</h4>");
        html.append(summaryTableHtml(new String[][]{
                {"Error Message", safe(result.getMessage())},
                {"Scenario NO", safe(result.getScenarioNo())},
                {"Scenario ACTION", safe(result.getScenarioAction())},
                {"Testcase", safe(result.getTestcaseName())},
                {"Excel Row", String.valueOf(result.getExcelRowNumber())},
                {"Function", safe(result.getFunctionName())},
                {"Object", safe(result.getObjectName())},
                {"Application", safe(result.getApplication())},
                {"Raw Value", maskedRawValue(result)},
                {"Resolved Value", maskedResolvedValue(result)},
                {"Raw XPath", safe(result.getRawXpath())},
                {"Resolved XPath", safe(result.getResolvedXpath())},
                {"Executed By", executedBy(result)},
                {"Screenshot", safe(evidence)}
        }));
        html.append("</div>");
        return html.toString();
    }

    private String screenshotLink(String screenshotPath, String label) {
        String relativePath = toReportRelativePath(screenshotPath);
        return "<a href='" + escapeAttribute(relativePath) + "' target='_blank'>" + escape(label) + "</a>";
    }

    private String toReportRelativePath(String screenshotPath) {
        try {
            Path reportDir = Path.of(FrameworkConstants.EXCEL_REPORT_DIR).toAbsolutePath();
            Path screenshot = Path.of(screenshotPath).toAbsolutePath();
            return reportDir.relativize(screenshot).toString().replace('\\', '/');
        } catch (RuntimeException exception) {
            return screenshotPath.replace('\\', '/');
        }
    }

    private String screenshotLabel(ExecutionResult result) {
        if ("screenshot".equalsIgnoreCase(safe(result.getFunctionName()).trim())) {
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
