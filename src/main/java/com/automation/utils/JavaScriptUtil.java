package com.automation.utils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public final class JavaScriptUtil {

    private JavaScriptUtil() {
    }

    public static void scrollToElement(WebDriver driver, WebElement element) {
        getExecutor(driver).executeScript("arguments[0].scrollIntoView({block:'center', inline:'nearest'});", element);
    }

    public static void clickUsingJavaScript(WebDriver driver, WebElement element) {
        getExecutor(driver).executeScript("arguments[0].click();", element);
    }

    public static boolean isPageLoaded(WebDriver driver) {
        Object readyState = getExecutor(driver).executeScript("return document.readyState");
        return "complete".equals(readyState);
    }

    private static JavascriptExecutor getExecutor(WebDriver driver) {
        if (!(driver instanceof JavascriptExecutor javascriptExecutor)) {
            throw new IllegalArgumentException("Driver does not support JavaScript execution.");
        }
        return javascriptExecutor;
    }
}
