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
        openUrl(step.getResolvedValue(), step);
    }

    public void openUrl(String url) {
        openUrl(url, currentContext());
    }

    protected void openUrl(String url, ResolvedStepContext step) {
        validateRequired(url, "openUrl", "URL", step);
        logExecuting("openUrl", step, "", null);
        driver.get(url.trim());
        logCompleted("openUrl", step);
    }

    public void click() {
        ResolvedStepContext step = StepContextHolder.get();
        click(step.getResolvedXPath(), step);
    }

    public void click(String xpath) {
        click(xpath, currentContext());
    }

    protected void click(String xpath, ResolvedStepContext step) {
        validateXPath(xpath, "click", step);
        logExecuting("click", step, xpath, null);
        waitForClickableElement(xpath, "click").click();
        logCompleted("click", step);
    }

    public void input() {
        ResolvedStepContext step = StepContextHolder.get();
        input(step.getResolvedXPath(), step.getResolvedValue(), step);
    }

    public void input(String xpath, String value) {
        input(xpath, value, currentContext());
    }

    protected void input(String xpath, String value, ResolvedStepContext step) {
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

    public void clear(String xpath) {
        validateXPath(xpath, "clear");
        LOGGER.info("Executing keyword clear on XPath: {}", xpath);
        waitForVisibleElement(xpath, "clear").clear();
        LOGGER.info("Completed keyword clear.");
    }

    public String getText(String xpath) {
        validateXPath(xpath, "getText");
        LOGGER.info("Executing keyword getText on XPath: {}", xpath);
        String text = waitForVisibleElement(xpath, "getText").getText();
        LOGGER.info("Completed keyword getText.");
        return text;
    }

    public void verifyDisplayed() {
        ResolvedStepContext step = StepContextHolder.get();
        verifyDisplayed(step.getResolvedXPath(), step);
    }

    public void verifyDisplayed(String xpath) {
        verifyDisplayed(xpath, currentContext());
    }

    protected void verifyDisplayed(String xpath, ResolvedStepContext step) {
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
        verifyText(step.getResolvedXPath(), step.getResolvedValue(), step);
    }

    public void verifyText(String xpath, String expectedText) {
        verifyText(xpath, expectedText, currentContext());
    }

    protected void verifyText(String xpath, String expectedText, ResolvedStepContext step) {
        validateXPath(xpath, "verifyText", step);
        validateRequired(expectedText, "verifyText", "Expected text", step);
        logExecuting("verifyText", step, xpath, null);
        String actual = getText(xpath);
        if (!expectedText.equals(actual)) {
            throw new AssertionError(withStepContext(
                    "Expected text '" + expectedText + "' but found '" + actual + "'. XPath: " + xpath,
                    step
            ));
        }
        logCompleted("verifyText", step);
    }

    public void verifyTextContains(String xpath, String expectedText) {
        validateXPath(xpath, "verifyTextContains");
        validateRequired(expectedText, "verifyTextContains", "Expected text");
        String expected = expectedText;
        LOGGER.info("Executing keyword verifyTextContains on XPath: {}", xpath);
        String actual = getText(xpath);
        if (!actual.contains(expected)) {
            throw new AssertionError("Expected text to contain '" + expected + "' but found '" + actual + "'. XPath: " + xpath);
        }
        LOGGER.info("Completed keyword verifyTextContains.");
    }

    public void verifyUrlContains(String expectedValue) {
        validateRequired(expectedValue, "verifyUrlContains", "Expected value");
        String actualUrl = driver.getCurrentUrl();
        if (actualUrl == null || !actualUrl.contains(expectedValue.trim())) {
            throw new AssertionError("Expected URL to contain '" + expectedValue.trim() + "' but actual URL was '" + actualUrl + "'.");
        }
    }

    public void verifyTitle(String expectedTitle) {
        validateRequired(expectedTitle, "verifyTitle", "Expected title");
        String actualTitle = driver.getTitle();
        if (!expectedTitle.trim().equals(actualTitle)) {
            throw new AssertionError("Expected title '" + expectedTitle.trim() + "' but actual title was '" + actualTitle + "'.");
        }
    }

    public void verifyTitleContains(String expectedTitlePart) {
        validateRequired(expectedTitlePart, "verifyTitleContains", "Expected title");
        String actualTitle = driver.getTitle();
        if (actualTitle == null || !actualTitle.contains(expectedTitlePart.trim())) {
            throw new AssertionError("Expected title to contain '" + expectedTitlePart.trim() + "' but actual title was '" + actualTitle + "'.");
        }
    }

    public WebElement waitVisible(String xpath) {
        validateXPath(xpath, "waitVisible");
        return waitForVisibleElement(xpath, "waitVisible");
    }

    public WebElement waitClickable(String xpath) {
        validateXPath(xpath, "waitClickable");
        return waitForClickableElement(xpath, "waitClickable");
    }

    public void scrollToElement(String xpath) {
        validateXPath(xpath, "scrollToElement");
        LOGGER.info("Executing keyword scrollToElement on XPath: {}", xpath);
        JavaScriptUtil.scrollToElement(driver, waitForVisibleElement(xpath, "scrollToElement"));
        LOGGER.info("Completed keyword scrollToElement.");
    }

    public void safeClick(String xpath) {
        validateXPath(xpath, "safeClick");
        LOGGER.info("Executing keyword safeClick on XPath: {}", xpath);
        try {
            click(xpath);
        } catch (ElementClickInterceptedException | StaleElementReferenceException | TimeoutException exception) {
            clickUsingJavaScript(xpath, exception);
        } catch (WebDriverException exception) {
            clickUsingJavaScript(xpath, exception);
        }
        LOGGER.info("Completed keyword safeClick.");
    }

    public void pressEnter(String xpath) {
        validateXPath(xpath, "pressEnter");
        LOGGER.info("Executing keyword pressEnter on XPath: {}", xpath);
        waitForVisibleElement(xpath, "pressEnter").sendKeys(Keys.ENTER);
        LOGGER.info("Completed keyword pressEnter.");
    }

    public boolean isDisplayed(String xpath) {
        validateXPath(xpath, "isDisplayed");
        try {
            return waitForVisibleElement(xpath, "isDisplayed").isDisplayed();
        } catch (AssertionError exception) {
            return false;
        }
    }

    public boolean isNotDisplayed(String xpath) {
        return !isDisplayed(xpath);
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

    private void validateXPath(String xpath, String keyword) {
        validateXPath(xpath, keyword, currentContext());
    }

    private void validateXPath(String xpath, String keyword, ResolvedStepContext step) {
        validateRequired(xpath, keyword, "XPath", step);
    }

    private void validateRequired(String value, String keyword, String fieldName) {
        validateRequired(value, keyword, fieldName, currentContext());
    }

    private void validateRequired(
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

    private ResolvedStepContext currentContext() {
        return StepContextHolder.current().orElse(null);
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
