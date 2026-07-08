package com.automation.engine;

import com.automation.context.StepContextHolder;
import com.automation.excel.DataReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.exceptions.ErrorContext;
import com.automation.config.ExcelExecutionConfig;
import com.automation.models.ExecutionResult;
import com.automation.models.KeywordExecutionResult;
import com.automation.models.ResolvedStepContext;
import com.automation.reports.ExcelReportConfig;
import com.automation.reports.SensitiveDataMasker;
import com.automation.services.ScreenshotService;
import com.automation.utils.WaitUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class KeywordEngine {

    private static final Logger LOGGER = LogManager.getLogger(KeywordEngine.class);
    private static final SensitiveDataMasker SENSITIVE_DATA_MASKER = new SensitiveDataMasker();
    private static final String MANUAL_SCREENSHOT_DISABLED_MESSAGE = "Manual screenshot skipped because report.manualScreenshotEnabled=false.";

    private final DataReader dataReader;
    private final ObjectRepositoryReader objectRepositoryReader;
    private final KeywordResolver keywordResolver;
    private final WebDriver driver;
    private final ExcelReportConfig reportConfig;
    private final ExcelExecutionConfig executionConfig;
    private final ScreenshotService screenshotService;

    public KeywordEngine(
            DataReader dataReader,
            ObjectRepositoryReader objectRepositoryReader,
            KeywordResolver keywordResolver
    ) {
        this(dataReader, objectRepositoryReader, keywordResolver, ExcelReportConfig.fromConfig());
    }

    public KeywordEngine(
            DataReader dataReader,
            ObjectRepositoryReader objectRepositoryReader,
            KeywordResolver keywordResolver,
            ExcelReportConfig reportConfig
    ) {
        this(dataReader, objectRepositoryReader, keywordResolver, reportConfig, ExcelExecutionConfig.load());
    }

    public KeywordEngine(
            DataReader dataReader,
            ObjectRepositoryReader objectRepositoryReader,
            KeywordResolver keywordResolver,
            ExcelReportConfig reportConfig,
            ExcelExecutionConfig executionConfig
    ) {
        this(dataReader, objectRepositoryReader, keywordResolver, reportConfig, executionConfig, null);
    }

    public KeywordEngine(
            DataReader dataReader,
            ObjectRepositoryReader objectRepositoryReader,
            KeywordResolver keywordResolver,
            ExcelReportConfig reportConfig,
            ExcelExecutionConfig executionConfig,
            ScreenshotService screenshotService
    ) {
        if (dataReader == null) {
            throw new IllegalArgumentException("DataReader must not be null.");
        }
        if (objectRepositoryReader == null) {
            throw new IllegalArgumentException("ObjectRepositoryReader must not be null.");
        }
        if (keywordResolver == null) {
            throw new IllegalArgumentException("KeywordResolver must not be null.");
        }
        this.dataReader = dataReader;
        this.objectRepositoryReader = objectRepositoryReader;
        this.keywordResolver = keywordResolver;
        this.driver = keywordResolver.getDriver();
        this.reportConfig = reportConfig == null ? ExcelReportConfig.fromConfig() : reportConfig;
        this.executionConfig = executionConfig == null ? ExcelExecutionConfig.load() : executionConfig;
        this.screenshotService = screenshotService == null
                ? new ScreenshotService(this.executionConfig.getScreenshotOutputDirectory())
                : screenshotService;
    }

    public ExecutionResult execute(ResolvedStepContext step) {
        if (step == null) {
            throw new IllegalArgumentException("ResolvedStepContext must not be null.");
        }

        StepContextHolder.set(step);
        WaitUtil.setTimeoutSeconds(executionConfig.getTimeoutSeconds());
        try {
            logResolvedStepStarted(step);

            ExecutionResult result;
            if (isBlank(step.getKeyword())) {
                result = ExecutionResult.failure(
                        step,
                        KeywordEngine.class.getName(),
                        "ENGINE",
                        failureMessage(
                                "Keyword is required in sheet " + safe(step.getSheetName())
                                        + " row " + step.getExcelRow() + ".",
                                step,
                                null
                        )
                );
            } else if (isScreenshotKeyword(step.getKeyword())) {
                result = executeManualScreenshot(step);
            } else if (isScreenshotPartByObjectKeyword(step.getKeyword())) {
                result = executeScreenshotPartByObject(step);
            } else {
                result = executeResolvedKeyword(step);
            }
            return logResolvedResult(result);
        } finally {
            WaitUtil.clearTimeoutSeconds();
            StepContextHolder.clear();
        }
    }

    DataReader getDataReader() {
        return dataReader;
    }

    ObjectRepositoryReader getObjectRepositoryReader() {
        return objectRepositoryReader;
    }

    private ExecutionResult executeResolvedKeyword(ResolvedStepContext step) {
        try {
            KeywordExecutionResult keywordResult = keywordResolver.execute(
                    step.getApplication(),
                    step.getKeyword()
            );
            String executedBy = isBlank(step.getExecutedBy())
                    ? keywordResult.getExecutedByClass()
                    : step.getExecutedBy();
            return ExecutionResult.success(
                    step,
                    executedBy,
                    keywordResult.getSourceType().name(),
                    keywordResult.getMessage()
            );
        } catch (RuntimeException | AssertionError exception) {
            String message = "Keyword '" + safe(step.getKeyword()) + "' failed at step row "
                    + step.getExcelRow() + ".";
            if (!containsStepContext(exception.getMessage())) {
                message += System.lineSeparator() + resolvedStepContext(step);
            }
            message += System.lineSeparator() + "Cause: " + safe(exception.getMessage());
            return ExecutionResult.failure(step, safe(step.getExecutedBy()), "KEYWORD", message);
        }
    }

    private ExecutionResult executeManualScreenshot(ResolvedStepContext step) {
        String executedBy = isBlank(step.getExecutedBy())
                ? KeywordEngine.class.getName()
                : step.getExecutedBy();
        if (!reportConfig.isManualScreenshotEnabled()) {
            return ExecutionResult.skipped(
                    step,
                    executedBy,
                    "REPORT",
                    MANUAL_SCREENSHOT_DISABLED_MESSAGE,
                    MANUAL_SCREENSHOT_DISABLED_MESSAGE
            );
        }

        try {
            String label = isBlank(step.getResolvedValue()) ? "ManualScreenshot" : step.getResolvedValue();
            String screenshotPath = screenshotService.capture(driver, screenshotBaseName(step, label));
            String evidence = screenshotPath == null
                    ? "Screenshot not available: driver does not support screenshots."
                    : screenshotPath;
            return ExecutionResult.success(
                    step,
                    executedBy,
                    "REPORT",
                    evidence,
                    "Manual screenshot captured."
            );
        } catch (RuntimeException exception) {
            String message = failureMessage(
                    "Failed to capture manual screenshot for step row " + step.getExcelRow() + ".",
                    step,
                    exception
            );
            return ExecutionResult.failure(step, executedBy, "REPORT", message);
        }
    }

    private ExecutionResult executeScreenshotPartByObject(ResolvedStepContext step) {
        String executedBy = isBlank(step.getExecutedBy())
                ? KeywordEngine.class.getName()
                : step.getExecutedBy();
        if (!reportConfig.isManualScreenshotEnabled()) {
            return ExecutionResult.skipped(
                    step,
                    executedBy,
                    "REPORT",
                    MANUAL_SCREENSHOT_DISABLED_MESSAGE,
                    MANUAL_SCREENSHOT_DISABLED_MESSAGE
            );
        }

        try {
            WebElement element = WaitUtil.waitForVisible(driver, By.xpath(safe(step.getResolvedXPath())));
            String label = isBlank(step.getResolvedValue())
                    ? safe(step.getObjectName())
                    : step.getResolvedValue();
            if (isBlank(label)) {
                label = "ObjectScreenshot";
            }
            List<String> screenshotPaths = screenshotService.captureElementInParts(
                    driver,
                    element,
                    screenshotBaseName(step, label)
            );
            String evidence = screenshotPaths.isEmpty()
                    ? "Screenshot not available: driver does not support object screenshots."
                    : String.join(System.lineSeparator(), screenshotPaths);
            return ExecutionResult.success(
                    step,
                    executedBy,
                    "REPORT",
                    evidence,
                    "Object screenshot captured in " + screenshotPaths.size() + " part(s)."
            );
        } catch (RuntimeException exception) {
            String message = failureMessage(
                    "Failed to capture object screenshot for step row " + step.getExcelRow() + ".",
                    step,
                    exception
            );
            return ExecutionResult.failure(step, executedBy, "REPORT", message);
        }
    }

    private ExecutionResult logResolvedResult(ExecutionResult result) {
        if (ExecutionResult.STATUS_SKIP.equals(result.getStatus())) {
            LOGGER.info(
                    "Keyword skipped. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Keyword = {}, "
                            + "Object = {}, Application = {}, XPath = {}, Status = {}, Source = {}, Message = {}",
                    result.getScenarioNo(),
                    result.getScenarioAction(),
                    result.getTestcaseName(),
                    result.getExcelRowNumber(),
                    result.getKeywordName(),
                    result.getObjectName(),
                    result.getApplication(),
                    result.getResolvedXPath(),
                    result.getStatus(),
                    result.getExecutionSource(),
                    result.getMessage()
            );
        } else if (result.isSuccess()) {
            LOGGER.info(
                    "Completed keyword. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Keyword = {}, "
                            + "Object = {}, Application = {}, XPath = {}, Status = {}, Source = {}",
                    result.getScenarioNo(),
                    result.getScenarioAction(),
                    result.getTestcaseName(),
                    result.getExcelRowNumber(),
                    result.getKeywordName(),
                    result.getObjectName(),
                    result.getApplication(),
                    result.getResolvedXPath(),
                    result.getStatus(),
                    result.getExecutionSource()
            );
        } else {
            LOGGER.error(
                    "Keyword failed. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Keyword = {}, "
                            + "Object = {}, Application = {}, XPath = {}, Status = {}, Source = {}, Message = {}",
                    result.getScenarioNo(),
                    result.getScenarioAction(),
                    result.getTestcaseName(),
                    result.getExcelRowNumber(),
                    result.getKeywordName(),
                    result.getObjectName(),
                    result.getApplication(),
                    result.getResolvedXPath(),
                    result.getStatus(),
                    result.getExecutionSource(),
                    result.getMessage()
            );
        }
        return result;
    }

    private void logResolvedStepStarted(ResolvedStepContext step) {
        String valueForLog = SENSITIVE_DATA_MASKER.maskIfNeeded(
                step.getResolvedValue(),
                false,
                step.getRawValue(),
                step.getObjectName(),
                step.getResolvedXPath(),
                step.getDescription(),
                step.getKeyword()
        );
        LOGGER.info(
                "Executing keyword. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Keyword = {}, "
                        + "Object = {}, Application = {}, XPath = {}, Value = {}",
                step.getScenarioNo(),
                step.getScenarioAction(),
                step.getTestcaseName(),
                step.getExcelRow(),
                step.getKeyword(),
                step.getObjectName(),
                step.getApplication(),
                step.getResolvedXPath(),
                valueForLog
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String resolvedStepContext(ResolvedStepContext step) {
        return new ErrorContext()
                .scenarioNo(step.getScenarioNo())
                .scenarioAction(step.getScenarioAction())
                .sheet(step.getSheetName())
                .testcase(step.getTestcaseName())
                .row(step.getExcelRow())
                .keyword(step.getKeyword())
                .object(step.getObjectName())
                .application(step.getApplication())
                .render();
    }

    private String failureMessage(String summary, ResolvedStepContext step, Throwable cause) {
        String message = summary + System.lineSeparator() + resolvedStepContext(step);
        if (cause != null) {
            message += System.lineSeparator() + "Cause: " + safe(cause.getMessage());
        }
        return message;
    }

    private boolean isScreenshotKeyword(String keywordName) {
        return keywordName != null && "screenshot".equalsIgnoreCase(keywordName.trim());
    }

    private boolean isScreenshotPartByObjectKeyword(String keywordName) {
        return keywordName != null && "screenshotPartByObject".equalsIgnoreCase(keywordName.trim());
    }

    private String screenshotBaseName(ResolvedStepContext step, String label) {
        return String.join(
                "_",
                safe(step.getScenarioNo()),
                safe(step.getTestcaseName()),
                "step" + step.getStepNumber(),
                "row" + step.getExcelRow(),
                safe(label)
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean containsStepContext(String message) {
        return message != null
                && (message.contains("Scenario NO:") || message.contains("Scenario ACTION:"));
    }
}
