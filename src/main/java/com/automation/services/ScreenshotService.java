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
    // Safety guard to prevent endless screenshot loops if a page keeps growing or scroll position stops updating.
    private static final int MAX_PART_SCREENSHOTS_PER_RUN = 100;

    private final Path outputDirectory;

    public ScreenshotService(Path outputDirectory) {
        this.outputDirectory = outputDirectory == null
                ? Path.of(FrameworkConstants.SCREENSHOT_DIR)
                : outputDirectory;
    }

    public String capture(WebDriver driver, String screenshotName) {
        return ScreenshotUtil.captureScreenshot(driver, screenshotName, outputDirectory);
    }

    public String capture(WebDriver driver, String screenshotName, String screenshotType) {
        return ScreenshotUtil.captureScreenshot(driver, screenshotName, screenshotType, outputDirectory);
    }

    public String captureScreen(WebDriver driver, ResolvedStepContext step, String label) {
        return capture(driver, screenshotBaseName(step), "Manual");
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
            while (partNumber <= MAX_PART_SCREENSHOTS_PER_RUN) {
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

    public List<String> captureFullPageInParts(WebDriver driver, ResolvedStepContext step, String label) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver must not be null.");
        }

        String screenshotLabel = fallbackLabel(label, fullPageLabel(step));
        if (!(driver instanceof JavascriptExecutor javascriptExecutor)) {
            String screenshotPath = captureScreen(driver, step, screenshotLabel + "_part1");
            return isBlank(screenshotPath) ? List.of() : List.of(screenshotPath);
        }

        List<String> screenshotPaths = new ArrayList<>();
        try {
            javascriptExecutor.executeScript(
                    "const scroller = document.scrollingElement || document.documentElement || document.body;"
                            + "scroller.scrollTop = 0;"
                            + "window.scrollTo(0, 0);"
            );
            pauseAfterScroll();

            int partNumber = 1;
            while (partNumber <= MAX_PART_SCREENSHOTS_PER_RUN) {
                String screenshotPath = captureScreen(driver, step, screenshotLabel + "_part" + partNumber);
                if (!isBlank(screenshotPath)) {
                    screenshotPaths.add(screenshotPath);
                }

                double beforeScrollTop = pageScrollTop(javascriptExecutor);
                double maxScrollTop = pageMaxScrollTop(javascriptExecutor);
                if (beforeScrollTop >= maxScrollTop - 1) {
                    break;
                }

                javascriptExecutor.executeScript(
                        "const scroller = document.scrollingElement || document.documentElement || document.body;"
                                + "scroller.scrollTop = Math.min(scroller.scrollTop + window.innerHeight, scroller.scrollHeight);"
                                + "window.scrollTo(0, scroller.scrollTop);"
                );
                pauseAfterScroll();

                double afterScrollTop = pageScrollTop(javascriptExecutor);
                if (afterScrollTop <= beforeScrollTop) {
                    LOGGER.debug("Stopping full-page screenshot loop because page scroll position did not move.");
                    break;
                }

                partNumber++;
            }

            if (partNumber > MAX_PART_SCREENSHOTS_PER_RUN) {
                LOGGER.warn("Stopped full-page screenshot capture after {} parts to avoid an endless loop.", MAX_PART_SCREENSHOTS_PER_RUN);
            }

            javascriptExecutor.executeScript(
                    "const scroller = document.scrollingElement || document.documentElement || document.body;"
                            + "scroller.scrollTop = 0;"
                            + "window.scrollTo(0, 0);"
            );
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Unable to capture full-page screenshots for: " + screenshotLabel, exception);
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

    public String fullPageLabel(ResolvedStepContext step) {
        return fallbackLabel(step == null ? "" : step.getDescription(), "FullPageScreenshot");
    }

    private String screenshotBaseName(ResolvedStepContext step) {
        if (step == null) {
            return "Screenshot";
        }
        return String.join(
                "_",
                safe(step.getScenarioNo()),
                scenarioLabel(step),
                stepNumberLabel(step.getStepNumber(), step.getExcelRow())
        );
    }

    private String scenarioLabel(ResolvedStepContext step) {
        String label = safe(step.getTestcaseName());
        if (!label.isBlank()) {
            return label;
        }
        label = safe(step.getScenarioAction());
        if (!label.isBlank()) {
            return label;
        }
        label = safe(step.getScenarioName());
        if (!label.isBlank()) {
            return label;
        }
        label = safe(step.getSheetName());
        return label.isBlank() ? "Scenario" : label;
    }

    private String stepNumberLabel(int stepNumber, int excelRow) {
        if (stepNumber > 0) {
            return String.valueOf(stepNumber);
        }
        if (excelRow > 0) {
            return String.valueOf(excelRow);
        }
        return "0";
    }

    protected void pauseAfterScroll() {
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

    private double pageScrollTop(JavascriptExecutor javascriptExecutor) {
        return number(javascriptExecutor.executeScript(
                "const scroller = document.scrollingElement || document.documentElement || document.body;"
                        + "return scroller.scrollTop || window.pageYOffset || 0;"
        ));
    }

    private double pageMaxScrollTop(JavascriptExecutor javascriptExecutor) {
        return number(javascriptExecutor.executeScript(
                "const scroller = document.scrollingElement || document.documentElement || document.body;"
                        + "return Math.max(0, (scroller.scrollHeight || 0) - (window.innerHeight || scroller.clientHeight || 0));"
        ));
    }
}
