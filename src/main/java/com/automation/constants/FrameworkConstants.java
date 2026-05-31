package com.automation.constants;

import java.nio.file.Paths;

public final class FrameworkConstants {

    private FrameworkConstants() {
    }

    public static final int DEFAULT_TIMEOUT_SECONDS = 10;
    public static final String CONFIG_FILE_PATH = Paths.get("src", "test", "resources", "config.properties").toString();
    public static final String LOGIN_TEST_DATA_RESOURCE = "testdata/login-data.json";
    public static final String SCREENSHOT_DIR = Paths.get(System.getProperty("user.dir"), "test-output", "screenshots").toString();
    public static final String EXTENT_REPORT_DIR = Paths.get(System.getProperty("user.dir"), "test-output", "extent-report").toString();
    public static final String EXTENT_REPORT_FILE = Paths.get(EXTENT_REPORT_DIR, "AutomationReport.html").toString();
}
