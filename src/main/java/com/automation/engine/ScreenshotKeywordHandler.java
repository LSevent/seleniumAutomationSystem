package com.automation.engine;

import com.automation.exceptions.ErrorContext;
import com.automation.models.ExecutionResult;
import com.automation.models.ResolvedStepContext;
import com.automation.reports.ExcelReportConfig;
import com.automation.services.ScreenshotEvidence;
import com.automation.services.ScreenshotService;
import com.automation.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ScreenshotKeywordHandler {

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
        return ScreenshotEvidence.supportsKeyword(keywordName);
    }

    public ExecutionResult execute(ResolvedStepContext step) {
        if (step == null) {
            throw new IllegalArgumentException("ResolvedStepContext must not be null.");
        }
        if (ScreenshotEvidence.isManualScreenshotKeyword(step.getKeyword())) {
            return executeManualScreenshot(step);
        }
        if (ScreenshotEvidence.isScreenshotPartByObjectKeyword(step.getKeyword())) {
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
            String label = ScreenshotEvidence.manualLabel(step);
            String screenshotPath = screenshotService.capture(driver, ScreenshotEvidence.baseName(step, label));
            String evidence = ScreenshotEvidence.evidenceOrUnavailable(
                    screenshotPath,
                    ScreenshotEvidence.MANUAL_SCREENSHOT_UNAVAILABLE_MESSAGE
            );
            return ExecutionResult.success(
                    step,
                    executedBy,
                    ScreenshotEvidence.REPORT_SOURCE,
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
            String label = ScreenshotEvidence.objectLabel(step);
            List<String> screenshotPaths = screenshotService.captureElementInParts(
                    driver,
                    element,
                    ScreenshotEvidence.baseName(step, label)
            );
            String evidence = ScreenshotEvidence.evidenceOrUnavailable(
                    screenshotPaths,
                    ScreenshotEvidence.OBJECT_SCREENSHOT_UNAVAILABLE_MESSAGE
            );
            return ExecutionResult.success(
                    step,
                    executedBy,
                    ScreenshotEvidence.REPORT_SOURCE,
                    evidence,
                    "Object screenshot captured in " + (screenshotPaths == null ? 0 : screenshotPaths.size()) + " part(s)."
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
                ScreenshotEvidence.REPORT_SOURCE,
                ScreenshotEvidence.MANUAL_SCREENSHOT_DISABLED_MESSAGE,
                ScreenshotEvidence.MANUAL_SCREENSHOT_DISABLED_MESSAGE
        );
    }

    private String executedBy(ResolvedStepContext step) {
        return isBlank(step.getExecutedBy())
                ? KeywordEngine.class.getName()
                : step.getExecutedBy();
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

}
