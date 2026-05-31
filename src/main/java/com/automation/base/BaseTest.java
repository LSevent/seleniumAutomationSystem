package com.automation.base;

import com.automation.config.ConfigReader;
import com.automation.constants.FrameworkConstants;
import com.automation.drivers.DriverFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.lang.reflect.Method;
import java.time.Duration;

public abstract class BaseTest {

    private static final Logger LOGGER = LogManager.getLogger(BaseTest.class);

    @BeforeSuite(alwaysRun = true)
    public void loadConfiguration() {
        ConfigReader.loadConfig();
        LOGGER.info("Framework configuration loaded.");
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        LOGGER.info("Starting setup for test: {}", method.getName());
        if (ConfigReader.getBooleanProperty("demoMode", true)) {
            LOGGER.warn("Demo mode is enabled. Browser setup is skipped until real application settings are configured.");
            return;
        }

        WebDriver driver = DriverFactory.initializeDriver();
        configureTimeouts(driver);
        maximizeBrowser(driver);
        openBaseUrl(driver);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverFactory.quitDriver();
    }

    protected WebDriver getDriver() {
        return DriverFactory.getDriver();
    }

    protected void skipIfDemoMode() {
        if (ConfigReader.getBooleanProperty("demoMode", true)) {
            throw new SkipException("Demo mode is enabled because the framework points to placeholder app settings. Set demoMode=false after updating baseUrl, locators, and test data.");
        }
    }

    private void configureTimeouts(WebDriver driver) {
        int timeout = ConfigReader.getIntProperty("timeout", FrameworkConstants.DEFAULT_TIMEOUT_SECONDS);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeout));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(timeout));
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        LOGGER.info("Timeouts configured. timeout={} seconds", timeout);
    }

    private void maximizeBrowser(WebDriver driver) {
        try {
            driver.manage().window().maximize();
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not maximize browser window. Continuing with current size.", exception);
        }
    }

    private void openBaseUrl(WebDriver driver) {
        String baseUrl = ConfigReader.getProperty("baseUrl");
        driver.get(baseUrl);
        LOGGER.info("URL opened: {}", baseUrl);
    }
}
