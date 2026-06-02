package com.automation.utils;

import com.automation.constants.FrameworkConstants;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ExtentReportManager {

    private static final ThreadLocal<ExtentTest> EXTENT_TEST = new ThreadLocal<>();
    private static ExtentReports extentReports;

    private ExtentReportManager() {
    }

    public static synchronized void initializeReport() {
        if (extentReports != null) {
            return;
        }

        try {
            Files.createDirectories(Path.of(FrameworkConstants.EXTENT_REPORT_DIR));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create Extent report directory.", exception);
        }

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(FrameworkConstants.EXTENT_REPORT_FILE);
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setDocumentTitle("Selenium Automation Report");
        sparkReporter.config().setReportName("UI Automation Test Results");

        extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);
        extentReports.setSystemInfo("Framework", "Selenium Java TestNG");
        extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));
        extentReports.setSystemInfo("Operating System", System.getProperty("os.name"));
    }

    public static synchronized void flushReport() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }

    public static synchronized ExtentTest createTest(String testName, String description) {
        initializeReport();
        ExtentTest test = extentReports.createTest(testName, description);
        EXTENT_TEST.set(test);
        return test;
    }

    public static synchronized ExtentTest createStandaloneTest(String testName, String description) {
        initializeReport();
        return extentReports.createTest(testName, description);
    }

    public static ExtentTest getTest() {
        return EXTENT_TEST.get();
    }

    public static void unloadTest() {
        EXTENT_TEST.remove();
    }

    public static String getReportFilePath() {
        return FrameworkConstants.EXTENT_REPORT_FILE;
    }
}
