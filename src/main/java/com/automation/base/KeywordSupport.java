package com.automation.base;

import com.automation.context.EvidenceContextHolder;
import com.automation.context.ScreenshotContextHolder;
import com.automation.context.StepContextHolder;
import com.automation.exceptions.ErrorContext;
import com.automation.models.ResolvedStepContext;
import com.automation.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public abstract class KeywordSupport {

    protected final WebDriver driver;

    protected KeywordSupport(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver must not be null.");
        }
        this.driver = driver;
    }

    protected WebElement waitForVisibleElement(String xpath, String keyword) {
        try {
            return WaitUtil.waitForVisible(driver, By.xpath(xpath.trim()));
        } catch (NoSuchElementException | TimeoutException | StaleElementReferenceException exception) {
            throw new AssertionError("Element not found for keyword " + keyword + ". XPath: " + xpath, exception);
        }
    }

    protected WebElement waitForClickableElement(String xpath, String keyword) {
        try {
            return WaitUtil.waitForClickable(driver, By.xpath(xpath.trim()));
        } catch (NoSuchElementException | TimeoutException | StaleElementReferenceException exception) {
            throw new AssertionError("Element not found for keyword " + keyword + ". XPath: " + xpath, exception);
        }
    }

    protected WebElement visibleElement(String keyword) {
        return waitForVisibleElement(xpath(), keyword);
    }

    protected WebElement clickableElement(String keyword) {
        return waitForClickableElement(xpath(), keyword);
    }

    protected void invisibleElement(String keyword) {
        String targetXPath = xpath();
        try {
            WaitUtil.waitForInvisible(driver, By.xpath(targetXPath.trim()));
        } catch (TimeoutException exception) {
            throw new AssertionError(
                    "Element is still displayed for keyword " + keyword + ". XPath: " + targetXPath,
                    exception
            );
        }
    }

    protected String visibleText(String keyword) {
        return visibleElement(keyword).getText();
    }

    protected ResolvedStepContext currentStep() {
        return StepContextHolder.get();
    }

    protected String xpath() {
        return currentStep().xpath();
    }

    protected String value() {
        return currentStep().value();
    }

    protected String rawValue() {
        return currentStep().rawValue();
    }

    protected String objectName() {
        return currentStep().objectName();
    }

    protected String application() {
        return currentStep().application();
    }

    protected String captureScreen() {
        return captureScreen(ScreenshotContextHolder.service().manualLabel(currentStep()));
    }

    protected String captureScreen(String label) {
        if (!ScreenshotContextHolder.isManualScreenshotEnabled()) {
            return "";
        }

        String screenshotPath = ScreenshotContextHolder.service().captureScreen(
                driver,
                currentStep(),
                label
        );
        registerEvidence(screenshotPath);
        return screenshotPath == null ? "" : screenshotPath;
    }

    protected List<String> captureObjectInParts() {
        return captureObjectInParts(ScreenshotContextHolder.service().objectLabel(currentStep()));
    }

    protected List<String> captureObjectInParts(String label) {
        return captureObjectInParts(
                visibleElement("screenshotPartByObject"),
                label
        );
    }

    protected List<String> captureObjectInParts(WebElement element, String label) {
        if (!ScreenshotContextHolder.isManualScreenshotEnabled()) {
            return List.of();
        }

        List<String> screenshotPaths = ScreenshotContextHolder.service().captureObjectInParts(
                driver,
                element,
                currentStep(),
                label
        );
        registerEvidence(screenshotPaths);
        return screenshotPaths;
    }

    protected List<String> captureFullPageInParts() {
        return captureFullPageInParts(ScreenshotContextHolder.service().fullPageLabel(currentStep()));
    }

    protected List<String> captureFullPageInParts(String label) {
        if (!ScreenshotContextHolder.isManualScreenshotEnabled()) {
            return List.of();
        }

        List<String> screenshotPaths = ScreenshotContextHolder.service().captureFullPageInParts(
                driver,
                currentStep(),
                label
        );
        registerEvidence(screenshotPaths);
        return screenshotPaths;
    }

    protected void registerEvidence(String evidencePath) {
        if (evidencePath != null && !evidencePath.isBlank()) {
            EvidenceContextHolder.add(evidencePath);
        }
    }

    protected void registerEvidence(List<String> evidencePaths) {
        if (evidencePaths == null) {
            return;
        }
        evidencePaths.forEach(this::registerEvidence);
    }

    protected String withStepContext(String message, ResolvedStepContext step) {
        String context = stepContext(step);
        return context.isBlank() ? message : message + System.lineSeparator() + context;
    }

    protected String withCurrentStepContext(String message) {
        return withStepContext(message, currentStep());
    }

    protected void failWithCurrentStepContext(String message) {
        throw new AssertionError(withCurrentStepContext(message));
    }

    private String stepContext(ResolvedStepContext step) {
        if (step == null) {
            return "";
        }
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
}
