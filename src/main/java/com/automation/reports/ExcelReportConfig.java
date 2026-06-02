package com.automation.reports;

import com.automation.config.ConfigReader;

public class ExcelReportConfig {

    private static final boolean DEFAULT_SHOW_SENSITIVE_DATA = false;
    private static final boolean DEFAULT_SCREENSHOT_ON_FAILURE = true;
    private static final boolean DEFAULT_MANUAL_SCREENSHOT_ENABLED = true;

    private final boolean showSensitiveData;
    private final boolean screenshotOnFailure;
    private final boolean manualScreenshotEnabled;

    public ExcelReportConfig(
            boolean showSensitiveData,
            boolean screenshotOnFailure,
            boolean manualScreenshotEnabled
    ) {
        this.showSensitiveData = showSensitiveData;
        this.screenshotOnFailure = screenshotOnFailure;
        this.manualScreenshotEnabled = manualScreenshotEnabled;
    }

    public static ExcelReportConfig fromConfig() {
        return new ExcelReportConfig(
                ConfigReader.getBooleanProperty("report.showSensitiveData", DEFAULT_SHOW_SENSITIVE_DATA),
                ConfigReader.getBooleanProperty("report.screenshotOnFailure", DEFAULT_SCREENSHOT_ON_FAILURE),
                ConfigReader.getBooleanProperty("report.manualScreenshotEnabled", DEFAULT_MANUAL_SCREENSHOT_ENABLED)
        );
    }

    public boolean isShowSensitiveData() {
        return showSensitiveData;
    }

    public boolean isScreenshotOnFailure() {
        return screenshotOnFailure;
    }

    public boolean isManualScreenshotEnabled() {
        return manualScreenshotEnabled;
    }
}
