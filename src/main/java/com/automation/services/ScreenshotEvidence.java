package com.automation.services;

import com.automation.models.ResolvedStepContext;

import java.util.List;

public final class ScreenshotEvidence {

    public static final String REPORT_SOURCE = "REPORT";
    public static final String MANUAL_SCREENSHOT_KEYWORD = "screenshot";
    public static final String SCREENSHOT_PART_BY_OBJECT_KEYWORD = "screenshotPartByObject";
    public static final String MANUAL_SCREENSHOT_DISABLED_MESSAGE =
            "Manual screenshot skipped because report.manualScreenshotEnabled=false.";
    public static final String MANUAL_SCREENSHOT_UNAVAILABLE_MESSAGE =
            "Screenshot not available: driver does not support screenshots.";
    public static final String OBJECT_SCREENSHOT_UNAVAILABLE_MESSAGE =
            "Screenshot not available: driver does not support object screenshots.";

    private ScreenshotEvidence() {
    }

    public static boolean supportsKeyword(String keyword) {
        return isManualScreenshotKeyword(keyword) || isScreenshotPartByObjectKeyword(keyword);
    }

    public static boolean isManualScreenshotKeyword(String keyword) {
        return MANUAL_SCREENSHOT_KEYWORD.equalsIgnoreCase(safe(keyword));
    }

    public static boolean isScreenshotPartByObjectKeyword(String keyword) {
        return SCREENSHOT_PART_BY_OBJECT_KEYWORD.equalsIgnoreCase(safe(keyword));
    }

    public static String manualLabel(ResolvedStepContext step) {
        return fallbackLabel(step == null ? "" : step.getDescription(), "ManualScreenshot");
    }

    public static String objectLabel(ResolvedStepContext step) {
        return fallbackLabel(
                step == null ? "" : step.getDescription(),
                fallbackLabel(step == null ? "" : step.getObjectName(), "ObjectScreenshot")
        );
    }

    public static String baseName(ResolvedStepContext step, String label) {
        if (step == null) {
            return fallbackLabel(label, "Screenshot");
        }
        return String.join(
                "_",
                safe(step.getScenarioNo()),
                safe(step.getTestcaseName()),
                "step" + step.getStepNumber(),
                "row" + step.getExcelRow(),
                fallbackLabel(label, "Screenshot")
        );
    }

    public static String evidenceOrUnavailable(String screenshotPath, String unavailableMessage) {
        return isBlank(screenshotPath) ? safe(unavailableMessage) : screenshotPath;
    }

    public static String evidenceOrUnavailable(List<String> screenshotPaths, String unavailableMessage) {
        if (screenshotPaths == null || screenshotPaths.isEmpty()) {
            return safe(unavailableMessage);
        }
        return String.join(System.lineSeparator(), screenshotPaths);
    }

    public static String fallbackLabel(String preferredLabel, String fallbackLabel) {
        return isBlank(preferredLabel) ? safe(fallbackLabel) : preferredLabel.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
