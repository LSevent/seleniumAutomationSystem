package com.automation.base;

import com.automation.context.StepContextHolder;
import com.automation.drivers.DriverFactory;
import com.automation.exceptions.ErrorContext;
import com.automation.exceptions.FrameworkException;
import com.automation.models.ResolvedStepContext;
import com.automation.reports.SensitiveDataMasker;
import com.automation.utils.JavaScriptUtil;
import com.automation.utils.WaitUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

public class BaseFunction {

    private static final Logger LOGGER = LogManager.getLogger(BaseFunction.class);
    private static final SensitiveDataMasker SENSITIVE_DATA_MASKER = new SensitiveDataMasker();

    private final WebDriver driver;

    public BaseFunction() {
        this(DriverFactory.getDriver());
    }

    public BaseFunction(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver must not be null.");
        }
        this.driver = driver;
    }

    public void openUrl() {
        ResolvedStepContext step = StepContextHolder.get();
        executeOpenUrl(step.getResolvedValue(), step);
    }

    protected void executeOpenUrl(String url, ResolvedStepContext step) {
        validateRequired(url, "openUrl", "URL", step);
        logExecuting("openUrl", step, "", null);
        driver.get(url.trim());
        logCompleted("openUrl", step);
    }

    public void click() {
        ResolvedStepContext step = StepContextHolder.get();
        executeClick(step.getResolvedXPath(), step);
    }

    protected void executeClick(String xpath, ResolvedStepContext step) {
        validateXPath(xpath, "click", step);
        logExecuting("click", step, xpath, null);
        waitForClickableElement(xpath, "click").click();
        logCompleted("click", step);
    }

    public void input() {
        ResolvedStepContext step = StepContextHolder.get();
        executeInput(step.getResolvedXPath(), step.getResolvedValue(), step);
    }

    protected void executeInput(String xpath, String value, ResolvedStepContext step) {
        validateXPath(xpath, "input", step);
        validateRequired(value, "input", "Value", step);
        String safeValue = value;
        logExecuting("input", step, xpath, maskValueIfNeeded(xpath, safeValue, step));
        WebElement element = waitForVisibleElement(xpath, "input");
        try {
            element.clear();
        } catch (RuntimeException exception) {
            LOGGER.debug("Could not clear element before input. XPath: {}", xpath, exception);
        }
        element.sendKeys(safeValue);
        logCompleted("input", step);
    }

    public void clear() {
        ResolvedStepContext step = StepContextHolder.get();
        executeClear(step.getResolvedXPath(), step);
    }

    protected void executeClear(String xpath, ResolvedStepContext step) {
        validateXPath(xpath, "clear", step);
        logExecuting("clear", step, xpath, null);
        waitForVisibleElement(xpath, "clear").clear();
        logCompleted("clear", step);
    }

    public String getText() {
        ResolvedStepContext step = StepContextHolder.get();
        return executeGetText(step.getResolvedXPath(), step);
    }

    protected String executeGetText(String xpath, ResolvedStepContext step) {
        validateXPath(xpath, "getText", step);
        logExecuting("getText", step, xpath, null);
        String text = waitForVisibleElement(xpath, "getText").getText();
        logCompleted("getText", step);
        return text;
    }

    public void verifyDisplayed() {
        ResolvedStepContext step = StepContextHolder.get();
        executeVerifyDisplayed(step.getResolvedXPath(), step);
    }

    protected void executeVerifyDisplayed(String xpath, ResolvedStepContext step) {
        validateXPath(xpath, "verifyDisplayed", step);
        logExecuting("verifyDisplayed", step, xpath, null);
        WebElement element = waitForVisibleElement(xpath, "verifyDisplayed");
        if (!element.isDisplayed()) {
            throw new AssertionError(withStepContext("Element is not displayed. XPath: " + xpath, step));
        }
        logCompleted("verifyDisplayed", step);
    }

    public void verifyText() {
        ResolvedStepContext step = StepContextHolder.get();
        executeVerifyText(step.getResolvedXPath(), step.getResolvedValue(), step);
    }

    protected void executeVerifyText(String xpath, String expectedText, ResolvedStepContext step) {
        validateXPath(xpath, "verifyText", step);
        validateRequired(expectedText, "verifyText", "Expected text", step);
        logExecuting("verifyText", step, xpath, null);
        String actual = executeGetText(xpath, step);
        if (!expectedText.equals(actual)) {
            throw new AssertionError(withStepContext(
                    "Expected text '" + expectedText + "' but found '" + actual + "'. XPath: " + xpath,
                    step
            ));
        }
        logCompleted("verifyText", step);
    }

    public void verifyTextContains() {
        ResolvedStepContext step = StepContextHolder.get();
        executeVerifyTextContains(step.getResolvedXPath(), step.getResolvedValue(), step);
    }

    protected void executeVerifyTextContains(
            String xpath,
            String expectedText,
            ResolvedStepContext step
    ) {
        validateXPath(xpath, "verifyTextContains", step);
        validateRequired(expectedText, "verifyTextContains", "Expected text", step);
        String expected = expectedText;
        logExecuting("verifyTextContains", step, xpath, null);
        String actual = executeGetText(xpath, step);
        if (!actual.contains(expected)) {
            throw new AssertionError(withStepContext(
                    "Expected text to contain '" + expected + "' but found '" + actual + "'. XPath: " + xpath,
                    step
            ));
        }
        logCompleted("verifyTextContains", step);
    }

    public void verifyUrlContains() {
        ResolvedStepContext step = StepContextHolder.get();
        executeVerifyUrlContains(step.getResolvedValue(), step);
    }

    protected void executeVerifyUrlContains(String expectedValue, ResolvedStepContext step) {
        validateRequired(expectedValue, "verifyUrlContains", "Expected value", step);
        String actualUrl = driver.getCurrentUrl();
        if (actualUrl == null || !actualUrl.contains(expectedValue.trim())) {
            throw new AssertionError(withStepContext(
                    "Expected URL to contain '" + expectedValue.trim() + "' but actual URL was '" + actualUrl + "'.",
                    step
            ));
        }
    }

    public void verifyTitle() {
        ResolvedStepContext step = StepContextHolder.get();
        executeVerifyTitle(step.getResolvedValue(), step);
    }

    protected void executeVerifyTitle(String expectedTitle, ResolvedStepContext step) {
        validateRequired(expectedTitle, "verifyTitle", "Expected title", step);
        String actualTitle = driver.getTitle();
        if (!expectedTitle.trim().equals(actualTitle)) {
            throw new AssertionError(withStepContext(
                    "Expected title '" + expectedTitle.trim() + "' but actual title was '" + actualTitle + "'.",
                    step
            ));
        }
    }

    public void verifyTitleContains() {
        ResolvedStepContext step = StepContextHolder.get();
        executeVerifyTitleContains(step.getResolvedValue(), step);
    }

    protected void executeVerifyTitleContains(String expectedTitlePart, ResolvedStepContext step) {
        validateRequired(expectedTitlePart, "verifyTitleContains", "Expected title", step);
        String actualTitle = driver.getTitle();
        if (actualTitle == null || !actualTitle.contains(expectedTitlePart.trim())) {
            throw new AssertionError(withStepContext(
                    "Expected title to contain '" + expectedTitlePart.trim() + "' but actual title was '" + actualTitle + "'.",
                    step
            ));
        }
    }

    public WebElement waitVisible() {
        ResolvedStepContext step = StepContextHolder.get();
        return executeWaitVisible(step.getResolvedXPath(), step);
    }

    protected WebElement executeWaitVisible(String xpath, ResolvedStepContext step) {
        validateXPath(xpath, "waitVisible", step);
        return waitForVisibleElement(xpath, "waitVisible");
    }

    public WebElement waitClickable() {
        ResolvedStepContext step = StepContextHolder.get();
        return executeWaitClickable(step.getResolvedXPath(), step);
    }

    protected WebElement executeWaitClickable(String xpath, ResolvedStepContext step) {
        validateXPath(xpath, "waitClickable", step);
        return waitForClickableElement(xpath, "waitClickable");
    }

    public void scrollToElement() {
        ResolvedStepContext step = StepContextHolder.get();
        executeScrollToElement(step.getResolvedXPath(), step);
    }

    protected void executeScrollToElement(String xpath, ResolvedStepContext step) {
        validateXPath(xpath, "scrollToElement", step);
        logExecuting("scrollToElement", step, xpath, null);
        JavaScriptUtil.scrollToElement(driver, waitForVisibleElement(xpath, "scrollToElement"));
        logCompleted("scrollToElement", step);
    }

    public void safeClick() {
        ResolvedStepContext step = StepContextHolder.get();
        executeSafeClick(step.getResolvedXPath(), step);
    }

    protected void executeSafeClick(String xpath, ResolvedStepContext step) {
        validateXPath(xpath, "safeClick", step);
        logExecuting("safeClick", step, xpath, null);
        try {
            executeClick(xpath, step);
        } catch (ElementClickInterceptedException | StaleElementReferenceException | TimeoutException exception) {
            clickUsingJavaScript(xpath, exception);
        } catch (WebDriverException exception) {
            clickUsingJavaScript(xpath, exception);
        }
        logCompleted("safeClick", step);
    }

    public void pressEnter() {
        ResolvedStepContext step = StepContextHolder.get();
        executePressEnter(step.getResolvedXPath(), step);
    }

    protected void executePressEnter(String xpath, ResolvedStepContext step) {
        validateXPath(xpath, "pressEnter", step);
        logExecuting("pressEnter", step, xpath, null);
        waitForVisibleElement(xpath, "pressEnter").sendKeys(Keys.ENTER);
        logCompleted("pressEnter", step);
    }

    public boolean isDisplayed() {
        ResolvedStepContext step = StepContextHolder.get();
        return executeIsDisplayed(step.getResolvedXPath(), step);
    }

    protected boolean executeIsDisplayed(String xpath, ResolvedStepContext step) {
        validateXPath(xpath, "isDisplayed", step);
        try {
            return waitForVisibleElement(xpath, "isDisplayed").isDisplayed();
        } catch (AssertionError exception) {
            return false;
        }
    }

    public boolean isNotDisplayed() {
        ResolvedStepContext step = StepContextHolder.get();
        return !executeIsDisplayed(step.getResolvedXPath(), step);
    }

    private void clickUsingJavaScript(String xpath, RuntimeException originalException) {
        try {
            WebElement element = waitForVisibleElement(xpath, "safeClick");
            JavaScriptUtil.scrollToElement(driver, element);
            JavaScriptUtil.clickUsingJavaScript(driver, element);
        } catch (RuntimeException fallbackException) {
            fallbackException.addSuppressed(originalException);
            throw new AssertionError("Safe click failed for XPath: " + xpath, fallbackException);
        }
    }

    private WebElement waitForVisibleElement(String xpath, String keyword) {
        try {
            return WaitUtil.waitForVisible(driver, By.xpath(xpath.trim()));
        } catch (NoSuchElementException | TimeoutException | StaleElementReferenceException exception) {
            throw new AssertionError("Element not found for keyword " + keyword + ". XPath: " + xpath, exception);
        }
    }

    private WebElement waitForClickableElement(String xpath, String keyword) {
        try {
            return WaitUtil.waitForClickable(driver, By.xpath(xpath.trim()));
        } catch (NoSuchElementException | TimeoutException | StaleElementReferenceException exception) {
            throw new AssertionError("Element not found for keyword " + keyword + ". XPath: " + xpath, exception);
        }
    }

    private void validateXPath(String xpath, String keyword, ResolvedStepContext step) {
        validateRequired(xpath, keyword, "XPath", step);
    }

    protected void validateRequired(
            String value,
            String keyword,
            String fieldName,
            ResolvedStepContext step
    ) {
        if (value == null || value.isBlank()) {
            String message = fieldName + " is required for keyword '" + keyword + "'.";
            throw new FrameworkException(withStepContext(message, step));
        }
    }

    private String withStepContext(String message, ResolvedStepContext step) {
        String context = stepContext(step);
        return context.isBlank() ? message : message + System.lineSeparator() + context;
    }

    private String stepContext(ResolvedStepContext step) {
        if (step == null) {
            return "";
        }
        return new ErrorContext()
                .scenarioNo(step.getScenarioNo())
                .scenarioAction(step.getScenarioAction())
                .testcase(step.getTestcaseName())
                .row(step.getExcelRow())
                .function(step.getFunction())
                .object(step.getObjectName())
                .application(step.getApplication())
                .render();
    }

    private void logExecuting(
            String keyword,
            ResolvedStepContext step,
            String xpath,
            String valueForLog
    ) {
        if (step == null) {
            if (valueForLog != null) {
                LOGGER.info("Executing keyword {} on XPath: {} with value: {}", keyword, xpath, valueForLog);
            } else if (xpath != null && !xpath.isBlank()) {
                LOGGER.info("Executing keyword {} on XPath: {}", keyword, xpath);
            } else {
                LOGGER.info("Executing keyword {}.", keyword);
            }
            return;
        }

        String details = logContext(step);
        if (xpath != null && !xpath.isBlank()) {
            details += ", XPath: " + xpath;
        }
        if (valueForLog != null) {
            details += ", Value: " + valueForLog;
        }
        LOGGER.info("Executing {}. {}", keyword, details);
    }

    private void logCompleted(String keyword, ResolvedStepContext step) {
        if (step == null) {
            LOGGER.info("Completed keyword {}.", keyword);
            return;
        }
        LOGGER.info("Completed {}. {}", keyword, logContext(step));
    }

    private String logContext(ResolvedStepContext step) {
        return "Scenario: " + safe(step.getScenarioNo())
                + ", Scenario Action: " + safe(step.getScenarioAction())
                + ", Testcase: " + safe(step.getTestcaseName())
                + ", Row: " + step.getExcelRow()
                + ", Function: " + safe(step.getFunction())
                + ", Object: " + safe(step.getObjectName())
                + ", Application: " + safe(step.getApplication());
    }

    private String maskValueIfNeeded(String xpath, String value, ResolvedStepContext step) {
        if (step != null && SENSITIVE_DATA_MASKER.isSensitive(
                step.getRawValue(),
                step.getObjectName(),
                xpath,
                step.getDescription(),
                step.getFunction()
        )) {
            return SensitiveDataMasker.MASK;
        }
        if (xpath != null && xpath.toLowerCase().contains("password")) {
            return SensitiveDataMasker.MASK;
        }
        return value == null ? "" : value;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
