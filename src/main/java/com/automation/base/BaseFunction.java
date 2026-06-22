package com.automation.base;

import com.automation.context.StepContextHolder;
import com.automation.drivers.DriverFactory;
import com.automation.exceptions.ErrorContext;
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
        driver.get(value().trim());
    }

    public void click() {
        waitForClickableElement(xpath(), "click").click();
    }

    public void input() {
        String targetXPath = xpath();
        String inputValue = value();
        WebElement element = waitForVisibleElement(targetXPath, "input");
        try {
            element.clear();
        } catch (RuntimeException exception) {
            LOGGER.debug("Could not clear element before input. XPath: {}", targetXPath, exception);
        }
        element.sendKeys(inputValue);
    }

    public void clear() {
        waitForVisibleElement(xpath(), "clear").clear();
    }

    public String getText() {
        return waitForVisibleElement(xpath(), "getText").getText();
    }

    public void verifyDisplayed() {
        String targetXPath = xpath();
        WebElement element = waitForVisibleElement(targetXPath, "verifyDisplayed");
        if (!element.isDisplayed()) {
            throw new AssertionError(withCurrentStepContext("Element is not displayed. XPath: " + targetXPath));
        }
    }

    public void verifyText() {
        String targetXPath = xpath();
        String expectedText = value();
        String actual = getText();
        if (!expectedText.equals(actual)) {
            throw new AssertionError(withCurrentStepContext(
                    "Expected text '" + expectedText + "' but found '" + actual + "'. XPath: " + targetXPath
            ));
        }
    }

    public void verifyTextContains() {
        String targetXPath = xpath();
        String expected = value();
        String actual = getText();
        if (!actual.contains(expected)) {
            throw new AssertionError(withCurrentStepContext(
                    "Expected text to contain '" + expected + "' but found '" + actual + "'. XPath: " + targetXPath
            ));
        }
    }

    public void verifyUrlContains() {
        String expectedValue = value();
        String actualUrl = driver.getCurrentUrl();
        if (actualUrl == null || !actualUrl.contains(expectedValue.trim())) {
            throw new AssertionError(withCurrentStepContext(
                    "Expected URL to contain '" + expectedValue.trim() + "' but actual URL was '" + actualUrl + "'."
            ));
        }
    }

    public void verifyTitle() {
        String expectedTitle = value();
        String actualTitle = driver.getTitle();
        if (!expectedTitle.trim().equals(actualTitle)) {
            throw new AssertionError(withCurrentStepContext(
                    "Expected title '" + expectedTitle.trim() + "' but actual title was '" + actualTitle + "'."
            ));
        }
    }

    public void verifyTitleContains() {
        String expectedTitlePart = value();
        String actualTitle = driver.getTitle();
        if (actualTitle == null || !actualTitle.contains(expectedTitlePart.trim())) {
            throw new AssertionError(withCurrentStepContext(
                    "Expected title to contain '" + expectedTitlePart.trim() + "' but actual title was '" + actualTitle + "'."
            ));
        }
    }

    public WebElement waitVisible() {
        return waitForVisibleElement(xpath(), "waitVisible");
    }

    public WebElement waitClickable() {
        return waitForClickableElement(xpath(), "waitClickable");
    }

    public void scrollToElement() {
        JavaScriptUtil.scrollToElement(driver, waitForVisibleElement(xpath(), "scrollToElement"));
    }

    public void safeClick() {
        String targetXPath = xpath();
        try {
            click();
        } catch (ElementClickInterceptedException | StaleElementReferenceException | TimeoutException exception) {
            clickUsingJavaScript(targetXPath, exception);
        } catch (WebDriverException exception) {
            clickUsingJavaScript(targetXPath, exception);
        }
    }

    public void pressEnter() {
        waitForVisibleElement(xpath(), "pressEnter").sendKeys(Keys.ENTER);
    }

    public boolean isDisplayed() {
        String targetXPath = xpath();
        try {
            return waitForVisibleElement(targetXPath, "isDisplayed").isDisplayed();
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
        return currentStep().object();
    }

    protected String application() {
        return currentStep().app();
    }

    private String withStepContext(String message, ResolvedStepContext step) {
        String context = stepContext(step);
        return context.isBlank() ? message : message + System.lineSeparator() + context;
    }

    private String withCurrentStepContext(String message) {
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
                .function(step.getFunction())
                .object(step.getObjectName())
                .application(step.getApplication())
                .render();
    }

}
