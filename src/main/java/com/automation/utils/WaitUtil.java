package com.automation.utils;

import com.automation.config.ConfigReader;
import com.automation.constants.FrameworkConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public final class WaitUtil {

    private WaitUtil() {
    }

    public static WebElement waitForVisible(WebDriver driver, By locator) {
        return createWait(driver).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(WebDriver driver, By locator) {
        return createWait(driver).until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static WebElement waitForPresence(WebDriver driver, By locator) {
        return createWait(driver).until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public static boolean waitForTitleContains(WebDriver driver, String title) {
        return createWait(driver).until(ExpectedConditions.titleContains(title));
    }

    private static WebDriverWait createWait(WebDriver driver) {
        int timeout = ConfigReader.getIntProperty("timeout", FrameworkConstants.DEFAULT_TIMEOUT_SECONDS);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        wait.ignoring(StaleElementReferenceException.class);
        return wait;
    }
}
