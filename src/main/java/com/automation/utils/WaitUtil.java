package com.automation.utils;

import com.automation.config.ConfigReader;
import com.automation.constants.FrameworkConstants;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public final class WaitUtil {

    private static final ThreadLocal<Integer> TIMEOUT_SECONDS = new ThreadLocal<>();

    private WaitUtil() {
    }

    public static void setTimeoutSeconds(int timeoutSeconds) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Timeout must be greater than zero. Actual value: " + timeoutSeconds);
        }
        TIMEOUT_SECONDS.set(timeoutSeconds);
    }

    public static void clearTimeoutSeconds() {
        TIMEOUT_SECONDS.remove();
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

    public static boolean waitForInvisible(WebDriver driver, By locator) {
        return createWait(driver).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static boolean waitForTitleContains(WebDriver driver, String title) {
        return createWait(driver).until(ExpectedConditions.titleContains(title));
    }

    public static boolean isVisibleWithin(WebDriver driver, By locator, int timeoutSeconds) {
        try {
            createWait(driver, timeoutSeconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    private static WebDriverWait createWait(WebDriver driver) {
        return createWait(driver, timeoutSeconds());
    }

    private static WebDriverWait createWait(WebDriver driver, int timeoutSeconds) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Timeout must be greater than zero. Actual value: " + timeoutSeconds);
        }
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        wait.ignoring(StaleElementReferenceException.class);
        return wait;
    }

    private static int timeoutSeconds() {
        Integer timeout = TIMEOUT_SECONDS.get();
        return timeout == null
                ? ConfigReader.getIntProperty("timeout", FrameworkConstants.DEFAULT_TIMEOUT_SECONDS)
                : timeout;
    }
}
