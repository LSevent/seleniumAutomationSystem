package com.automation.listeners;

import com.automation.drivers.DriverFactory;
import com.automation.utils.ExtentReportManager;
import com.automation.utils.ScreenshotUtil;
import com.aventstack.extentreports.ExtentTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener {

    private static final Logger LOGGER = LogManager.getLogger(TestListener.class);

    @Override
    public void onStart(ITestContext context) {
        ExtentReportManager.initializeReport();
        LOGGER.info("Test suite started: {}", context.getSuite().getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentReportManager.flushReport();
        LOGGER.info("Test suite finished: {}", context.getSuite().getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        ExtentReportManager.createTest(testName, description);
        LOGGER.info("Test started: {}", testName);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        ExtentTest test = ExtentReportManager.getTest();
        if (test != null) {
            test.pass("Test passed: " + testName);
        }
        LOGGER.info("Test passed: {}", testName);
        ExtentReportManager.unloadTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        Throwable throwable = result.getThrowable();
        ExtentTest test = ExtentReportManager.getTest();

        if (test != null) {
            test.fail(throwable);
        }

        WebDriver driver = DriverFactory.getNullableDriver();
        if (driver != null && test != null) {
            String screenshotPath = ScreenshotUtil.captureScreenshot(driver, testName);
            if (screenshotPath != null) {
                try {
                    test.addScreenCaptureFromPath(screenshotPath);
                } catch (Exception exception) {
                    LOGGER.warn("Could not attach screenshot to Extent report: {}", screenshotPath, exception);
                }
            }
        }

        LOGGER.error("Test failed: {} | {}", testName, failureSummary(throwable));
        LOGGER.debug("Full TestNG failure for {}.", testName, throwable);
        ExtentReportManager.unloadTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        ExtentTest test = ExtentReportManager.getTest();
        if (test != null) {
            test.skip(result.getThrowable());
        }
        LOGGER.warn("Test skipped: {}", testName);
        ExtentReportManager.unloadTest();
    }

    private String failureSummary(Throwable throwable) {
        if (throwable == null) {
            return "No failure details were provided.";
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .limit(2)
                .reduce((first, second) -> first + " | " + second)
                .orElse(throwable.getClass().getSimpleName());
    }
}
