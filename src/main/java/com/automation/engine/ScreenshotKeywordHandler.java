package com.automation.engine;

import com.automation.exceptions.ErrorContext;
import com.automation.models.ExecutionResult;
import com.automation.models.ResolvedStepContext;
import com.automation.reports.ExcelReportConfig;
import com.automation.services.ScreenshotService;
import com.automation.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ScreenshotKeywordHandler {

    private static final String MANUAL_SCREENSHOT_DISABLED_MESSAGE =
            "Manual screenshot skipped because report.manualScreenshotEnabled=false.";

    private final WebDriver driver;
    private final ExcelReportConfig reportConfig;
    private final ScreenshotService screenshotService;

    public ScreenshotKeywordHandler(
            WebDriver driver,
            ExcelReportConfig reportConfig,
            ScreenshotService screenshotService
    ) {
        this.driver = driver;
        this.reportConfig = reportConfig == null ? ExcelReportConfig.fromConfig() : reportConfig;
        this.screenshotService = screenshotService;
    }

    public boolean supports(String keywordName) {
        return isManualScreenshotKeyword(keywordName) || isScreenshotPartByObjectKeyword(keywordName);
    }

    public ExecutionResult execute(ResolvedStepContext step) {
        if (step == null) {
            throw new IllegalArgumentException("ResolvedStepContext must not be null.");
        }
        if (isManualScreenshotKeyword(step.getKeyword())) {
            return executeManualScreenshot(step);
        }
        if (isScreenshotPartByObjectKeyword(step.getKeyword())) {
            return executeScreenshotPartByObject(step);
        }
        throw new IllegalArgumentException("Unsupported screenshot keyword: " + safe(step.getKeyword()));
    }

    private ExecutionResult executeManualScreenshot(ResolvedStepContext step) {
        String executedBy = executedBy(step);
        if (!reportConfig.isManualScreenshotEnabled()) {
            return skipped(step, executedBy);
        }

        try {
            String label = manualScreenshotLabel(step);
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
        String executedBy = executedBy(step);
        if (!reportConfig.isManualScreenshotEnabled()) {
            return skipped(step, executedBy);
        }

        try {
            WebElement element = WaitUtil.waitForVisible(driver, By.xpath(safe(step.getResolvedXPath())));
            String label = objectScreenshotLabel(step);
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

    private ExecutionResult skipped(ResolvedStepContext step, String executedBy) {
        return ExecutionResult.skipped(
                step,
                executedBy,
                "REPORT",
                MANUAL_SCREENSHOT_DISABLED_MESSAGE,
                MANUAL_SCREENSHOT_DISABLED_MESSAGE
        );
    }

    private String executedBy(ResolvedStepContext step) {
        return isBlank(step.getExecutedBy())
                ? KeywordEngine.class.getName()
                : step.getExecutedBy();
    }

    private boolean isManualScreenshotKeyword(String keywordName) {
        return keywordName != null && "screenshot".equalsIgnoreCase(keywordName.trim());
    }

    private boolean isScreenshotPartByObjectKeyword(String keywordName) {
        return keywordName != null && "screenshotPartByObject".equalsIgnoreCase(keywordName.trim());
    }

    private String manualScreenshotLabel(ResolvedStepContext step) {
        return fallbackLabel(step.getDescription(), "ManualScreenshot");
    }

    private String objectScreenshotLabel(ResolvedStepContext step) {
        return fallbackLabel(step.getDescription(), fallbackLabel(step.getObjectName(), "ObjectScreenshot"));
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

    private String failureMessage(String summary, ResolvedStepContext step, Throwable cause) {
        String message = summary + System.lineSeparator() + resolvedStepContext(step);
        if (cause != null) {
            message += System.lineSeparator() + "Cause: " + safe(cause.getMessage());
        }
        return message;
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String fallbackLabel(String preferredLabel, String fallbackLabel) {
        return isBlank(preferredLabel) ? safe(fallbackLabel) : preferredLabel.trim();
    }
}
