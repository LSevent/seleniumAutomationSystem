package com.automation.base;

import com.automation.drivers.DriverFactory;
import com.automation.utils.JavaScriptUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

public class BaseFunction extends KeywordSupport {

    private static final Logger LOGGER = LogManager.getLogger(BaseFunction.class);

    public BaseFunction() {
        this(DriverFactory.getDriver());
    }

    public BaseFunction(WebDriver driver) {
        super(driver);
    }

    public void openUrl() {
        driver.get(value().trim());
    }

    public void click() {
        clickableElement("click").click();
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
        visibleElement("clear").clear();
    }

    public String getText() {
        return visibleText("getText");
    }

    public void verifyDisplayed() {
        String targetXPath = xpath();
        WebElement element = waitForVisibleElement(targetXPath, "verifyDisplayed");
        if (!element.isDisplayed()) {
            failWithCurrentStepContext("Element is not displayed. XPath: " + targetXPath);
        }
    }

    public void verifyText() {
        verifyElementTextEquals(value());
    }

    public void verifyTextContains() {
        verifyElementTextContains(value());
    }

    public void verifyUrlContains() {
        assertContains("URL", value().trim(), driver.getCurrentUrl());
    }

    public void verifyTitle() {
        assertEquals("title", value().trim(), driver.getTitle());
    }

    public void verifyTitleContains() {
        assertContains("title", value().trim(), driver.getTitle());
    }

    public WebElement waitVisible() {
        return visibleElement("waitVisible");
    }

    public WebElement waitClickable() {
        return clickableElement("waitClickable");
    }

    public void scrollToElement() {
        JavaScriptUtil.scrollToElement(driver, visibleElement("scrollToElement"));
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
        visibleElement("pressEnter").sendKeys(Keys.ENTER);
    }

    public boolean isDisplayed() {
        try {
            return visibleElement("isDisplayed").isDisplayed();
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

    private void verifyElementTextEquals(String expectedText) {
        String targetXPath = xpath();
        String actual = waitForVisibleElement(targetXPath, "verifyText").getText();
        if (!expectedText.equals(actual)) {
            failWithCurrentStepContext(
                    "Expected text '" + expectedText + "' but found '" + actual + "'. XPath: " + targetXPath
            );
        }
    }

    private void verifyElementTextContains(String expected) {
        String targetXPath = xpath();
        String actual = waitForVisibleElement(targetXPath, "verifyTextContains").getText();
        if (!actual.contains(expected)) {
            failWithCurrentStepContext(
                    "Expected text to contain '" + expected + "' but found '" + actual + "'. XPath: " + targetXPath
            );
        }
    }

    private void assertEquals(String label, String expected, String actual) {
        if (!expected.equals(actual)) {
            failWithCurrentStepContext(
                    "Expected " + label + " '" + expected + "' but actual " + label + " was '" + actual + "'."
            );
        }
    }

    private void assertContains(String label, String expected, String actual) {
        if (actual == null || !actual.contains(expected)) {
            failWithCurrentStepContext(
                    "Expected " + label + " to contain '" + expected + "' but actual " + label + " was '" + actual + "'."
            );
        }
    }

}
