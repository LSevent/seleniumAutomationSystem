package com.automation.utils;

import com.automation.constants.FrameworkConstants;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ScreenshotUtil {

    private static final Logger LOGGER = LogManager.getLogger(ScreenshotUtil.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtil() {
    }

    public static String captureScreenshot(WebDriver driver, String testName) {
        return captureScreenshot(driver, testName, Path.of(FrameworkConstants.SCREENSHOT_DIR));
    }

    public static String captureScreenshot(WebDriver driver, String testName, Path screenshotDirectory) {
        if (!(driver instanceof TakesScreenshot takesScreenshot)) {
            LOGGER.warn("Driver does not support screenshots.");
            return null;
        }

        try {
            Path targetDirectory = screenshotDirectory == null
                    ? Path.of(FrameworkConstants.SCREENSHOT_DIR)
                    : screenshotDirectory;
            Files.createDirectories(targetDirectory);
            String fileName = sanitizeFileName(testName) + "_" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + ".png";
            Path destination = targetDirectory.resolve(fileName);
            File source = takesScreenshot.getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(source, destination.toFile());
            LOGGER.info("Screenshot captured: {}", destination.toAbsolutePath());
            return destination.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to capture screenshot for test: " + testName, exception);
        }
    }

    private static String sanitizeFileName(String value) {
        return value == null || value.isBlank() ? "test" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
