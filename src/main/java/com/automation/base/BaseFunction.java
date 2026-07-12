package com.automation.base;

import com.automation.drivers.DriverFactory;
import com.automation.utils.JavaScriptUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

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
        String targetXPath = xpath();
        try {
            waitForClickableElement(targetXPath, "click").click();
        } catch (WebDriverException exception) {
            clickUsingJavaScript(targetXPath, exception);
        }
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

    public void select() {
        new Select(visibleElement("select")).selectByVisibleText(value().trim());
    }

    public void verifyDisplayed() {
        visibleElement("verifyDisplayed");
    }

    public void verifyNotDisplayed() {
        invisibleElement("verifyNotDisplayed");
    }

    public void verifyText() {
        String targetXPath = xpath();
        String expected = value();
        String actual = waitForVisibleElement(targetXPath, "verifyText").getText();
        if (!expected.equals(actual)) {
            failWithCurrentStepContext(
                    "Expected text '" + expected + "' but found '" + actual + "'. XPath: " + targetXPath
            );
        }
    }

    public void verifyTextContains() {
        String targetXPath = xpath();
        String expected = value();
        String actual = waitForVisibleElement(targetXPath, "verifyTextContains").getText();
        if (!actual.contains(expected)) {
            failWithCurrentStepContext(
                    "Expected text to contain '" + expected + "' but found '" + actual + "'. XPath: " + targetXPath
            );
        }
    }

    public void verifyUrlContains() {
        assertContains("URL", value().trim(), driver.getCurrentUrl());
    }

    public void verifyTitle() {
        String expected = value().trim();
        String actual = driver.getTitle();
        if (!expected.equals(actual)) {
            failWithCurrentStepContext(
                    "Expected title '" + expected + "' but actual title was '" + actual + "'."
            );
        }
    }

    public void verifyTitleContains() {
        assertContains("title", value().trim(), driver.getTitle());
    }

    public void scrollToElement() {
        JavaScriptUtil.scrollToElement(driver, visibleElement("scrollToElement"));
    }

    public void pressEnter() {
        visibleElement("pressEnter").sendKeys(Keys.ENTER);
    }

    public void screenshot() {
        captureScreen();
    }

    public void screenshotPartByObject() {
        captureObjectInParts();
    }

    public void screenshotFullPart() {
        captureFullPageInParts();
    }

    private void clickUsingJavaScript(String xpath, RuntimeException originalException) {
        try {
            WebElement element = waitForVisibleElement(xpath, "click");
            JavaScriptUtil.scrollToElement(driver, element);
            JavaScriptUtil.clickUsingJavaScript(driver, element);
        } catch (RuntimeException fallbackException) {
            fallbackException.addSuppressed(originalException);
            throw new AssertionError("Click failed for XPath: " + xpath, fallbackException);
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
