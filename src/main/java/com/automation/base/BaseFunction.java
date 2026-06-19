package com.automation.base;

import com.automation.context.StepContextHolder;
import com.automation.drivers.DriverFactory;
import com.automation.exceptions.ErrorContext;
import com.automation.exceptions.FrameworkException;
import com.automation.models.ResolvedStepContext;
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
        ResolvedStepContext step = currentStep();
        String url = requireValue(step, "openUrl", "URL");
        driver.get(url.trim());
    }

    public void click() {
        ResolvedStepContext step = currentStep();
        String xpath = requireXPath(step, "click");
        waitForClickableElement(xpath, "click").click();
    }

    public void input() {
        ResolvedStepContext step = currentStep();
        String xpath = requireXPath(step, "input");
        String value = requireValue(step, "input", "Value");
        String safeValue = value;
        WebElement element = waitForVisibleElement(xpath, "input");
        try {
            element.clear();
        } catch (RuntimeException exception) {
            LOGGER.debug("Could not clear element before input. XPath: {}", xpath, exception);
        }
        element.sendKeys(safeValue);
    }

    public void clear() {
        ResolvedStepContext step = currentStep();
        String xpath = requireXPath(step, "clear");
        waitForVisibleElement(xpath, "clear").clear();
    }

    public String getText() {
        ResolvedStepContext step = currentStep();
        String xpath = requireXPath(step, "getText");
        String text = waitForVisibleElement(xpath, "getText").getText();
        return text;
    }

    public void verifyDisplayed() {
        ResolvedStepContext step = currentStep();
        String xpath = requireXPath(step, "verifyDisplayed");
        WebElement element = waitForVisibleElement(xpath, "verifyDisplayed");
        if (!element.isDisplayed()) {
            throw new AssertionError(withStepContext("Element is not displayed. XPath: " + xpath, step));
        }
    }

    public void verifyText() {
        ResolvedStepContext step = currentStep();
        String xpath = requireXPath(step, "verifyText");
        String expectedText = requireValue(step, "verifyText", "Expected text");
        String actual = getText();
        if (!expectedText.equals(actual)) {
            throw new AssertionError(withStepContext(
                    "Expected text '" + expectedText + "' but found '" + actual + "'. XPath: " + xpath,
                    step
            ));
        }
    }

    public void verifyTextContains() {
        ResolvedStepContext step = currentStep();
        String xpath = requireXPath(step, "verifyTextContains");
        String expectedText = requireValue(step, "verifyTextContains", "Expected text");
        String expected = expectedText;
        String actual = getText();
        if (!actual.contains(expected)) {
            throw new AssertionError(withStepContext(
                    "Expected text to contain '" + expected + "' but found '" + actual + "'. XPath: " + xpath,
                    step
            ));
        }
    }

    public void verifyUrlContains() {
        ResolvedStepContext step = currentStep();
        String expectedValue = requireValue(step, "verifyUrlContains", "Expected value");
        String actualUrl = driver.getCurrentUrl();
        if (actualUrl == null || !actualUrl.contains(expectedValue.trim())) {
            throw new AssertionError(withStepContext(
                    "Expected URL to contain '" + expectedValue.trim() + "' but actual URL was '" + actualUrl + "'.",
                    step
            ));
        }
    }

    public void verifyTitle() {
        ResolvedStepContext step = currentStep();
        String expectedTitle = requireValue(step, "verifyTitle", "Expected title");
        String actualTitle = driver.getTitle();
        if (!expectedTitle.trim().equals(actualTitle)) {
            throw new AssertionError(withStepContext(
                    "Expected title '" + expectedTitle.trim() + "' but actual title was '" + actualTitle + "'.",
                    step
            ));
        }
    }

    public void verifyTitleContains() {
        ResolvedStepContext step = currentStep();
        String expectedTitlePart = requireValue(step, "verifyTitleContains", "Expected title");
        String actualTitle = driver.getTitle();
        if (actualTitle == null || !actualTitle.contains(expectedTitlePart.trim())) {
            throw new AssertionError(withStepContext(
                    "Expected title to contain '" + expectedTitlePart.trim() + "' but actual title was '" + actualTitle + "'.",
                    step
            ));
        }
    }

    public WebElement waitVisible() {
        ResolvedStepContext step = currentStep();
        String xpath = requireXPath(step, "waitVisible");
        return waitForVisibleElement(xpath, "waitVisible");
    }

    public WebElement waitClickable() {
        ResolvedStepContext step = currentStep();
        String xpath = requireXPath(step, "waitClickable");
        return waitForClickableElement(xpath, "waitClickable");
    }

    public void scrollToElement() {
        ResolvedStepContext step = currentStep();
        String xpath = requireXPath(step, "scrollToElement");
        JavaScriptUtil.scrollToElement(driver, waitForVisibleElement(xpath, "scrollToElement"));
    }

    public void safeClick() {
        ResolvedStepContext step = currentStep();
        String xpath = requireXPath(step, "safeClick");
        try {
            click();
        } catch (ElementClickInterceptedException | StaleElementReferenceException | TimeoutException exception) {
            clickUsingJavaScript(xpath, exception);
        } catch (WebDriverException exception) {
            clickUsingJavaScript(xpath, exception);
        }
    }

    public void pressEnter() {
        ResolvedStepContext step = currentStep();
        String xpath = requireXPath(step, "pressEnter");
        waitForVisibleElement(xpath, "pressEnter").sendKeys(Keys.ENTER);
    }

    public boolean isDisplayed() {
        ResolvedStepContext step = currentStep();
        String xpath = requireXPath(step, "isDisplayed");
        try {
            return waitForVisibleElement(xpath, "isDisplayed").isDisplayed();
        } catch (AssertionError exception) {
            return false;
        }
    }

    public boolean isNotDisplayed() {
        return !isDisplayed();
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

    protected ResolvedStepContext currentStep() {
        return StepContextHolder.get();
    }

    protected String requireXPath(ResolvedStepContext step, String keyword) {
        String xpath = step.getResolvedXPath();
        validateRequired(xpath, keyword, "XPath", step);
        return xpath;
    }

    protected String requireValue(ResolvedStepContext step, String keyword, String fieldName) {
        String value = step.getResolvedValue();
        validateRequired(value, keyword, fieldName, step);
        return value;
    }

    private void validateRequired(String value, String keyword, String fieldName, ResolvedStepContext step) {
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

}
