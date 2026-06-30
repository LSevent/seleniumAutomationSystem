package com.automation.tests;

import com.automation.config.ConfigReader;
import com.automation.config.ExcelExecutionConfig;
import com.automation.drivers.DriverFactory;
import com.automation.engine.KeywordEngine;
import com.automation.engine.KeywordResolver;
import com.automation.engine.ScenarioRunner;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.exceptions.FrameworkException;
import com.automation.models.ExecutionResult;
import com.automation.reports.ExcelExecutionReporter;
import com.automation.reports.ExcelReportConfig;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ConfiguredExcelExecutionTest {

    @Test
    public void runConfiguredExcelWorkbook() {
        ConfigReader.loadConfig();
        Properties excelProperties = loadExcelConfigProperties();
        requireConfiguredValue(
                excelProperties,
                ExcelExecutionConfig.SCENARIO_FILE_PATH_KEY,
                "Excel scenario file path is required. Please set excel.scenarioFilePath in src/test/resources/excelConfig.properties or pass:\n"
                        + "-Dexcel.scenarioFilePath=\"C:/path/to/scenarios.xlsx\""
        );
        requireConfiguredValue(
                excelProperties,
                ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY,
                "Report output directory is required. Please set report.outputDirectory in src/test/resources/excelConfig.properties or pass:\n"
                        + "-Dreport.outputDirectory=\"C:/path/to/reports\""
        );

        ExcelExecutionConfig executionConfig = ExcelExecutionConfig.load();
        validateConfiguredScenarioFile(executionConfig);
        executionConfig.validate();

        WebDriver driver = null;
        try (ExcelReader excelReader = new ExcelReader(executionConfig.getScenarioFilePath().toString())) {
            driver = DriverFactory.initializeDriver();
            configureDriver(driver);

            DataReader dataReader = new DataReader(excelReader);
            ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
            ExcelReportConfig reportConfig = ExcelReportConfig.fromConfig();
            KeywordEngine keywordEngine = new KeywordEngine(
                    dataReader,
                    objectRepositoryReader,
                    new KeywordResolver(driver),
                    reportConfig,
                    executionConfig
            );
            ExcelExecutionReporter reporter = new ExcelExecutionReporter(driver, reportConfig, executionConfig);
            ScenarioRunner runner = new ScenarioRunner(
                    new ScenarioReader(excelReader),
                    new StepReader(excelReader),
                    keywordEngine,
                    reporter
            );

            List<ExecutionResult> results = runner.runActiveScenarios();

            Assert.assertFalse(results.isEmpty(), "Configured Excel workbook did not execute any steps.");
            Assert.assertTrue(
                    results.stream().allMatch(ExecutionResult::isSuccess),
                    "Configured Excel workbook failed:\n" + failureMessages(results)
            );
            Assert.assertTrue(
                    Files.exists(executionConfig.getReportFilePath()),
                    "Expected Excel execution report was not created: " + executionConfig.getReportFilePath()
            );
        } finally {
            if (driver != null) {
                DriverFactory.quitDriver();
            }
        }
    }

    private static Properties loadExcelConfigProperties() {
        try (InputStream inputStream = openExcelConfigStream()) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (IOException exception) {
            throw new FrameworkException("Unable to load Excel execution configuration from " + ExcelExecutionConfig.CONFIG_FILE_NAME + ".", exception);
        }
    }

    private static InputStream openExcelConfigStream() throws IOException {
        InputStream classpathConfig = ConfiguredExcelExecutionTest.class
                .getClassLoader()
                .getResourceAsStream(ExcelExecutionConfig.CONFIG_FILE_NAME);
        if (classpathConfig != null) {
            return classpathConfig;
        }

        Path configPath = Path.of("src", "test", "resources", ExcelExecutionConfig.CONFIG_FILE_NAME);
        if (Files.exists(configPath)) {
            return Files.newInputStream(configPath);
        }

        throw new IOException(ExcelExecutionConfig.CONFIG_FILE_NAME + " was not found on the classpath or at "
                + configPath.toAbsolutePath());
    }

    private static void requireConfiguredValue(Properties properties, String key, String message) {
        if (configuredValue(properties, key).isBlank()) {
            throw new FrameworkException(message);
        }
    }

    private static String configuredValue(Properties properties, String key) {
        String systemValue = clean(System.getProperty(key));
        if (!systemValue.isBlank()) {
            return systemValue;
        }
        return clean(properties.getProperty(key));
    }

    private static void validateConfiguredScenarioFile(ExcelExecutionConfig executionConfig) {
        Path scenarioFile = executionConfig.getScenarioFilePath();
        if (!Files.exists(scenarioFile)) {
            throw new FrameworkException(
                    "Configured Excel scenario file was not found:\n"
                            + displayPath(scenarioFile)
                            + "\n\nPlease update excel.scenarioFilePath in src/test/resources/excelConfig.properties or pass:\n"
                            + "-Dexcel.scenarioFilePath=\"C:/path/to/scenarios.xlsx\""
            );
        }
    }

    private static void configureDriver(WebDriver driver) {
        int timeout = ConfigReader.getIntProperty("timeout", 10);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeout));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(timeout));
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            driver.manage().window().maximize();
        } catch (RuntimeException ignored) {
            // Some drivers or headless environments do not support maximize.
        }
    }

    private static String failureMessages(List<ExecutionResult> results) {
        return results.stream()
                .filter(result -> !result.isSuccess())
                .map(result -> "Row " + result.getExcelRowNumber()
                        + ", Keyword " + result.getKeywordName()
                        + ", Status " + result.getStatus()
                        + ": " + result.getMessage())
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private static String clean(String value) {
        String trimmedValue = value == null ? "" : value.trim();
        if (trimmedValue.length() >= 2
                && trimmedValue.startsWith("\"")
                && trimmedValue.endsWith("\"")) {
            return trimmedValue.substring(1, trimmedValue.length() - 1).trim();
        }
        return trimmedValue;
    }

    private static String displayPath(Path path) {
        return path == null ? "" : path.toString().replace('\\', '/');
    }
}
