package com.automation.base;

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

    protected String withStepContext(String message, ResolvedStepContext step) {
        String context = stepContext(step);
        return context.isBlank() ? message : message + System.lineSeparator() + context;
    }

    protected String withCurrentStepContext(String message) {
        return withStepContext(message, currentStep());
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
