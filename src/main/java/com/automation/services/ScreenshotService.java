package com.automation.services;

import com.automation.constants.FrameworkConstants;
import com.automation.utils.ScreenshotUtil;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScreenshotService {

    private static final Logger LOGGER = LogManager.getLogger(ScreenshotService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final Path outputDirectory;

    public ScreenshotService(Path outputDirectory) {
        this.outputDirectory = outputDirectory == null
                ? Path.of(FrameworkConstants.SCREENSHOT_DIR)
                : outputDirectory;
    }

    public String capture(WebDriver driver, String screenshotName) {
        return ScreenshotUtil.captureScreenshot(driver, screenshotName, outputDirectory);
    }

    public List<String> captureElementInParts(WebDriver driver, WebElement element, String screenshotName) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver must not be null.");
        }
        if (element == null) {
            throw new IllegalArgumentException("WebElement must not be null.");
        }
        if (!(driver instanceof TakesScreenshot takesScreenshot)) {
            LOGGER.warn("Driver does not support screenshots.");
            return List.of();
        }
        if (!(driver instanceof JavascriptExecutor javascriptExecutor)) {
            return captureElementFallback(element, screenshotName);
        }

        try {
            Files.createDirectories(outputDirectory);
            ElementMetrics firstMetrics = readElementMetrics(javascriptExecutor, element);
            if (!firstMetrics.isUsable()) {
                return captureElementFallback(element, screenshotName);
            }

            List<String> screenshotPaths = new ArrayList<>();
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            double partHeight = Math.max(1, firstMetrics.viewportHeight());
            int partNumber = 1;
            for (double offset = 0; offset < firstMetrics.height(); offset += partHeight) {
                javascriptExecutor.executeScript(
                        "window.scrollTo(arguments[0], arguments[1]);",
                        Math.max(0, firstMetrics.documentX()),
                        Math.max(0, firstMetrics.documentY() + offset)
                );
                ElementMetrics currentMetrics = readElementMetrics(javascriptExecutor, element);
                File source = takesScreenshot.getScreenshotAs(OutputType.FILE);
                BufferedImage screenshot = ImageIO.read(source);
                if (screenshot == null) {
                    throw new IOException("Screenshot image could not be read.");
                }

                Crop crop = cropFor(currentMetrics, screenshot);
                if (!crop.isUsable()) {
                    LOGGER.debug("Skipping unusable screenshot crop for element part {}.", partNumber);
                    continue;
                }

                BufferedImage partImage = screenshot.getSubimage(crop.x(), crop.y(), crop.width(), crop.height());
                Path destination = outputDirectory.resolve(fileName(screenshotName, "_part" + partNumber, timestamp));
                ImageIO.write(partImage, "png", destination.toFile());
                screenshotPaths.add(destination.toAbsolutePath().normalize().toString());
                LOGGER.info("Element screenshot part captured: {}", destination.toAbsolutePath());
                partNumber++;
            }

            if (screenshotPaths.isEmpty()) {
                return captureElementFallback(element, screenshotName);
            }
            return screenshotPaths;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to capture element screenshots for: " + screenshotName, exception);
        }
    }

    private List<String> captureElementFallback(WebElement element, String screenshotName) {
        try {
            Files.createDirectories(outputDirectory);
            File source = element.getScreenshotAs(OutputType.FILE);
            if (source == null) {
                LOGGER.warn("Element screenshot was not available.");
                return List.of();
            }
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            Path destination = outputDirectory.resolve(fileName(screenshotName, "_part1", timestamp));
            FileUtils.copyFile(source, destination.toFile());
            LOGGER.info("Element screenshot captured: {}", destination.toAbsolutePath());
            return List.of(destination.toAbsolutePath().normalize().toString());
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to capture element screenshot for: " + screenshotName, exception);
        }
    }

    private ElementMetrics readElementMetrics(JavascriptExecutor javascriptExecutor, WebElement element) {
        Object result = javascriptExecutor.executeScript(
                "const element = arguments[0];"
                        + "const rect = element.getBoundingClientRect();"
                        + "return {"
                        + "left: rect.left,"
                        + "top: rect.top,"
                        + "width: rect.width,"
                        + "height: rect.height,"
                        + "documentX: rect.left + window.pageXOffset,"
                        + "documentY: rect.top + window.pageYOffset,"
                        + "viewportWidth: window.innerWidth,"
                        + "viewportHeight: window.innerHeight,"
                        + "devicePixelRatio: window.devicePixelRatio || 1"
                        + "};",
                element
        );
        if (!(result instanceof Map<?, ?> values)) {
            return ElementMetrics.empty();
        }
        return new ElementMetrics(
                number(values.get("left")),
                number(values.get("top")),
                number(values.get("width")),
                number(values.get("height")),
                number(values.get("documentX")),
                number(values.get("documentY")),
                number(values.get("viewportWidth")),
                number(values.get("viewportHeight")),
                number(values.get("devicePixelRatio"))
        );
    }

    private Crop cropFor(ElementMetrics metrics, BufferedImage screenshot) {
        if (!metrics.isUsable()) {
            return Crop.empty();
        }

        double ratio = Math.max(1, metrics.devicePixelRatio());
        int imageWidth = screenshot.getWidth();
        int imageHeight = screenshot.getHeight();
        int left = clamp((int) Math.floor(Math.max(0, metrics.left()) * ratio), 0, imageWidth);
        int top = clamp((int) Math.floor(Math.max(0, metrics.top()) * ratio), 0, imageHeight);
        int right = clamp((int) Math.ceil(Math.min(metrics.viewportWidth(), metrics.left() + metrics.width()) * ratio), 0, imageWidth);
        int bottom = clamp((int) Math.ceil(Math.min(metrics.viewportHeight(), metrics.top() + metrics.height()) * ratio), 0, imageHeight);

        if (right <= left || bottom <= top) {
            return Crop.empty();
        }
        return new Crop(left, top, right - left, bottom - top);
    }

    private String fileName(String screenshotName, String suffix, String timestamp) {
        return sanitizeFileName(screenshotName) + suffix + "_" + timestamp + ".png";
    }

    private String sanitizeFileName(String value) {
        return value == null || value.isBlank() ? "screenshot" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ElementMetrics(
            double left,
            double top,
            double width,
            double height,
            double documentX,
            double documentY,
            double viewportWidth,
            double viewportHeight,
            double devicePixelRatio
    ) {
        private static ElementMetrics empty() {
            return new ElementMetrics(0, 0, 0, 0, 0, 0, 0, 0, 1);
        }

        private boolean isUsable() {
            return width > 0 && height > 0 && viewportWidth > 0 && viewportHeight > 0;
        }
    }

    private record Crop(int x, int y, int width, int height) {
        private static Crop empty() {
            return new Crop(0, 0, 0, 0);
        }

        private boolean isUsable() {
            return width > 0 && height > 0;
        }
    }
}
