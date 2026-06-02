package com.automation.services;

import com.automation.utils.ScreenshotUtil;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;

public class ScreenshotService {

    private final Path outputDirectory;

    public ScreenshotService(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String capture(WebDriver driver, String screenshotName) {
        return ScreenshotUtil.captureScreenshot(driver, screenshotName, outputDirectory);
    }
}
