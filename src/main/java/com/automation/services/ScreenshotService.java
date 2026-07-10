package com.automation.services;

import com.automation.constants.FrameworkConstants;
import com.automation.models.ResolvedStepContext;
import com.automation.utils.ScreenshotUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScreenshotService {

    private static final Logger LOGGER = LogManager.getLogger(ScreenshotService.class);
    private static final int SCROLL_PAUSE_MILLIS = 500;
    private static final int MAX_SCREENSHOT_PARTS = 100;

    private final Path outputDirectory;

    public ScreenshotService(Path outputDirectory) {
        this.outputDirectory = outputDirectory == null
                ? Path.of(FrameworkConstants.SCREENSHOT_DIR)
                : outputDirectory;
    }

    public String capture(WebDriver driver, String screenshotName) {
        return ScreenshotUtil.captureScreenshot(driver, screenshotName, outputDirectory);
    }

    public String captureScreen(WebDriver driver, ResolvedStepContext step) {
        return captureScreen(driver, step, manualLabel(step));
    }

    public String captureScreen(WebDriver driver, ResolvedStepContext step, String label) {
        return capture(driver, screenshotName(step, label, "ManualScreenshot"));
    }

    public List<String> captureObjectInParts(WebDriver driver, WebElement element, ResolvedStepContext step) {
        return captureObjectInParts(driver, element, step, objectLabel(step));
    }

    public List<String> captureObjectInParts(WebDriver driver, WebElement element, ResolvedStepContext step, String label) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver must not be null.");
        }
        if (element == null) {
            throw new IllegalArgumentException("WebElement must not be null.");
        }

        String screenshotLabel = fallbackLabel(label, objectLabel(step));
        if (!(driver instanceof JavascriptExecutor javascriptExecutor)) {
            String screenshotPath = captureScreen(driver, step, screenshotLabel + "_part1");
            return isBlank(screenshotPath) ? List.of() : List.of(screenshotPath);
        }

        List<String> screenshotPaths = new ArrayList<>();
        try {
            javascriptExecutor.executeScript("arguments[0].scrollIntoView({block: 'start'});", element);
            pauseAfterScroll();

            double elementScrollHeight = number(javascriptExecutor.executeScript(
                    "return arguments[0].scrollHeight || 0;",
                    element
            ));
            double elementClientHeight = number(javascriptExecutor.executeScript(
                    "return arguments[0].clientHeight || arguments[0].offsetHeight || 0;",
                    element
            ));
            boolean scrollElement = elementScrollHeight > elementClientHeight + 1;

            if (scrollElement) {
                javascriptExecutor.executeScript("arguments[0].scrollTop = 0;", element);
            }

            int partNumber = 1;
            while (partNumber <= MAX_SCREENSHOT_PARTS) {
                String screenshotPath = captureScreen(driver, step, screenshotLabel + "_part" + partNumber);
                if (!isBlank(screenshotPath)) {
                    screenshotPaths.add(screenshotPath);
                }

                if (scrollElement) {
                    double beforeScrollTop = number(javascriptExecutor.executeScript(
                            "return arguments[0].scrollTop || 0;",
                            element
                    ));
                    javascriptExecutor.executeScript(
                            "arguments[0].scrollTop = Math.min(arguments[0].scrollTop + arguments[0].clientHeight, arguments[0].scrollHeight);",
                            element
                    );
                    pauseAfterScroll();
                    double afterScrollTop = number(javascriptExecutor.executeScript(
                            "return arguments[0].scrollTop || 0;",
                            element
                    ));
                    if (afterScrollTop <= beforeScrollTop) {
                        break;
                    }
                } else {
                    double elementBottom = number(javascriptExecutor.executeScript(
                            "return arguments[0].getBoundingClientRect().bottom + window.pageYOffset;",
                            element
                    ));
                    double viewportBottom = number(javascriptExecutor.executeScript(
                            "return window.pageYOffset + window.innerHeight;"
                    ));
                    if (viewportBottom >= elementBottom - 1) {
                        break;
                    }
                    javascriptExecutor.executeScript("window.scrollBy(0, window.innerHeight);");
                    pauseAfterScroll();
                }

                partNumber++;
            }

            if (scrollElement) {
                javascriptExecutor.executeScript("arguments[0].scrollTop = 0;", element);
            } else {
                javascriptExecutor.executeScript("arguments[0].scrollIntoView({block: 'start'});", element);
            }
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Unable to capture object screenshots for: " + screenshotLabel, exception);
        }

        return screenshotPaths;
    }

    public String manualLabel(ResolvedStepContext step) {
        return fallbackLabel(step == null ? "" : step.getDescription(), "ManualScreenshot");
    }

    public String objectLabel(ResolvedStepContext step) {
        return fallbackLabel(
                step == null ? "" : step.getDescription(),
                fallbackLabel(step == null ? "" : step.getObjectName(), "ObjectScreenshot")
        );
    }

    private String screenshotName(ResolvedStepContext step, String label, String fallbackLabel) {
        if (step == null) {
            return fallbackLabel(label, "Screenshot");
        }
        return String.join(
                "_",
                safe(step.getScenarioNo()),
                safe(step.getTestcaseName()),
                "step" + step.getStepNumber(),
                "row" + step.getExcelRow(),
                fallbackLabel(label, fallbackLabel)
        );
    }

    private void pauseAfterScroll() {
        try {
            Thread.sleep(SCROLL_PAUSE_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting after screenshot scroll.", exception);
        }
    }

    private String fallbackLabel(String preferredLabel, String fallbackLabel) {
        return isBlank(preferredLabel) ? safe(fallbackLabel) : preferredLabel.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0;
    }
}
