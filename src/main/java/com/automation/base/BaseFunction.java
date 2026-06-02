package com.automation.base;

import com.automation.drivers.DriverFactory;
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

    public void openUrl(String url) {
        validateRequired(url, "openUrl", "URL");
        LOGGER.info("Executing keyword openUrl.");
        driver.get(url.trim());
        LOGGER.info("Completed keyword openUrl.");
    }

    public void click(String xpath) {
        validateXPath(xpath, "click");
        LOGGER.info("Executing keyword click on XPath: {}", xpath);
        waitForClickableElement(xpath, "click").click();
        LOGGER.info("Completed keyword click.");
    }

    public void input(String xpath, String value) {
        validateXPath(xpath, "input");
        validateRequired(value, "input", "Value");
        String safeValue = value;
        LOGGER.info("Executing keyword input on XPath: {} with value: {}", xpath, maskValueIfNeeded(xpath, safeValue));
        WebElement element = waitForVisibleElement(xpath, "input");
        try {
            element.clear();
        } catch (RuntimeException exception) {
            LOGGER.debug("Could not clear element before input. XPath: {}", xpath, exception);
        }
        element.sendKeys(safeValue);
        LOGGER.info("Completed keyword input.");
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

    public void verifyDisplayed(String xpath) {
        validateXPath(xpath, "verifyDisplayed");
        LOGGER.info("Executing keyword verifyDisplayed on XPath: {}", xpath);
        WebElement element = waitForVisibleElement(xpath, "verifyDisplayed");
        if (!element.isDisplayed()) {
            throw new AssertionError("Element is not displayed. XPath: " + xpath);
        }
        LOGGER.info("Completed keyword verifyDisplayed.");
    }

    public void verifyText(String xpath, String expectedText) {
        validateXPath(xpath, "verifyText");
        validateRequired(expectedText, "verifyText", "Expected text");
        String expected = expectedText;
        LOGGER.info("Executing keyword verifyText on XPath: {}", xpath);
        String actual = getText(xpath);
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected text '" + expected + "' but found '" + actual + "'. XPath: " + xpath);
        }
        LOGGER.info("Completed keyword verifyText.");
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
        validateRequired(xpath, keyword, "XPath");
    }

    private void validateRequired(String value, String keyword, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required for keyword '" + keyword + "'.");
        }
    }

    private String maskValueIfNeeded(String xpath, String value) {
        if (xpath != null && xpath.toLowerCase().contains("password")) {
            return "****";
        }
        return value == null ? "" : value;
    }
}
