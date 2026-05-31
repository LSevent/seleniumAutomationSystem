package com.automation.base;

import com.automation.utils.JavaScriptUtil;
import com.automation.utils.WaitUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

public abstract class BasePage {

    private static final Logger LOGGER = LogManager.getLogger(BasePage.class);
    protected final WebDriver driver;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
    }

    protected void click(By locator) {
        waitForClickable(locator).click();
        LOGGER.debug("Clicked element: {}", locator);
    }

    protected void type(By locator, String value) {
        WebElement element = waitForVisible(locator);
        element.clear();
        element.sendKeys(value == null ? "" : value);
        LOGGER.debug("Typed into element: {}", locator);
    }

    protected String getText(By locator) {
        String text = waitForVisible(locator).getText();
        LOGGER.debug("Read text from element: {}", locator);
        return text;
    }

    protected boolean isDisplayed(By locator) {
        try {
            return waitForVisible(locator).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException | TimeoutException exception) {
            LOGGER.debug("Element is not displayed: {}", locator);
            return false;
        }
    }

    protected WebElement waitForVisible(By locator) {
        return WaitUtil.waitForVisible(driver, locator);
    }

    protected WebElement waitForClickable(By locator) {
        return WaitUtil.waitForClickable(driver, locator);
    }

    protected void scrollToElement(By locator) {
        JavaScriptUtil.scrollToElement(driver, waitForVisible(locator));
        LOGGER.debug("Scrolled to element: {}", locator);
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    protected void safeClick(By locator) {
        try {
            click(locator);
        } catch (WebDriverException exception) {
            LOGGER.warn("Standard click failed for {}. Retrying with JavaScript click.", locator);
            WebElement element = waitForVisible(locator);
            JavaScriptUtil.clickUsingJavaScript(driver, element);
        }
    }
}
