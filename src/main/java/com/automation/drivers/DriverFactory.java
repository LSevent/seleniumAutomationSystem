package com.automation.drivers;

import com.automation.config.ConfigReader;
import com.automation.config.ExcelExecutionConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public final class DriverFactory {

    private static final Logger LOGGER = LogManager.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {
    }

    public static WebDriver initializeDriver() {
        return initializeDriver(
                ConfigReader.getProperty("browser", "chrome"),
                ConfigReader.getBooleanProperty("headless", false),
                ConfigReader.getBooleanProperty("remote", false),
                ConfigReader.getProperty("gridUrl", "http://localhost:4444/wd/hub")
        );
    }

    public static WebDriver initializeDriver(ExcelExecutionConfig executionConfig) {
        ExcelExecutionConfig config = executionConfig == null ? ExcelExecutionConfig.load() : executionConfig;
        return initializeDriver(
                config.getBrowser(),
                config.isHeadless(),
                config.isRemote(),
                config.getGridUrl()
        );
    }

    private static WebDriver initializeDriver(
            String configuredBrowser,
            boolean configuredHeadless,
            boolean remote,
            String gridUrl
    ) {
        if (DRIVER.get() != null) {
            return DRIVER.get();
        }

        String cleanBrowser = configuredBrowser == null || configuredBrowser.isBlank()
                ? "chrome"
                : configuredBrowser.trim().toLowerCase();
        boolean headless = configuredHeadless || cleanBrowser.contains("headless");
        String browser = normalizeBrowserName(cleanBrowser);

        WebDriver webDriver = remote ? createRemoteDriver(browser, headless, gridUrl) : createLocalDriver(browser, headless);
        DRIVER.set(webDriver);
        LOGGER.info("Browser launched. browser={}, headless={}, remote={}", browser, headless, remote);
        return webDriver;
    }

    public static WebDriver getDriver() {
        WebDriver webDriver = DRIVER.get();
        if (webDriver == null) {
            throw new IllegalStateException("WebDriver is not initialized for the current thread.");
        }
        return webDriver;
    }

    public static WebDriver getNullableDriver() {
        return DRIVER.get();
    }

    public static void quitDriver() {
        WebDriver webDriver = DRIVER.get();
        if (webDriver != null) {
            webDriver.quit();
            DRIVER.remove();
            LOGGER.info("Driver quit successfully.");
        }
    }

    private static WebDriver createLocalDriver(String browser, boolean headless) {
        return switch (browser) {
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                yield new FirefoxDriver(buildFirefoxOptions(headless));
            }
            case "edge" -> {
                WebDriverManager.edgedriver().setup();
                yield new EdgeDriver(buildEdgeOptions(headless));
            }
            case "chrome" -> {
                WebDriverManager.chromedriver().setup();
                yield new ChromeDriver(buildChromeOptions(headless));
            }
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser + ". Supported values: chrome, firefox, edge, chrome-headless.");
        };
    }

    private static WebDriver createRemoteDriver(String browser, boolean headless, String gridUrl) {
        String remoteUrl = gridUrl == null || gridUrl.isBlank()
                ? "http://localhost:4444/wd/hub"
                : gridUrl.trim();
        MutableCapabilities capabilities = switch (browser) {
            case "firefox" -> buildFirefoxOptions(headless);
            case "edge" -> buildEdgeOptions(headless);
            case "chrome" -> buildChromeOptions(headless);
            default -> throw new IllegalArgumentException("Unsupported remote browser: " + browser);
        };

        try {
            LOGGER.info("Creating RemoteWebDriver session at {}", remoteUrl);
            return new RemoteWebDriver(new URL(remoteUrl), capabilities);
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Invalid Selenium Grid URL: " + remoteUrl, exception);
        }
    }

    private static ChromeOptions buildChromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.setExperimentalOption("prefs", Map.of(
                "credentials_enable_service", false,
                "profile.password_manager_enabled", false,
                "profile.password_manager_leak_detection", false
        ));
        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
        }
        return options;
    }

    private static FirefoxOptions buildFirefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless");
        }
        return options;
    }

    private static EdgeOptions buildEdgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
        }
        return options;
    }

    private static String normalizeBrowserName(String browser) {
        if ("headless-chrome".equals(browser) || "chrome-headless".equals(browser)) {
            return "chrome";
        }
        return browser;
    }
}
