package com.automation.config;

import com.automation.exceptions.FrameworkException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public class ExcelExecutionConfig {

    public static final String CONFIG_FILE_NAME = "excelConfig.properties";
    public static final String SCENARIO_FILE_PATH_KEY = "excel.scenarioFilePath";
    public static final String REPORT_OUTPUT_DIRECTORY_KEY = "report.outputDirectory";
    public static final String REPORT_FILE_NAME_KEY = "report.fileName";
    public static final String SCREENSHOT_OUTPUT_DIRECTORY_KEY = "screenshot.outputDirectory";
    public static final String DEFAULT_REPORT_OUTPUT_DIRECTORY = "test-output/reports";
    public static final String DEFAULT_REPORT_FILE_NAME = "ExcelAutomationReport.html";

    private static final Logger LOGGER = LogManager.getLogger(ExcelExecutionConfig.class);

    private final Path scenarioFilePath;
    private final Path reportOutputDirectory;
    private final String reportFileName;
    private final Path reportFilePath;
    private final Path screenshotOutputDirectory;

    private ExcelExecutionConfig(
            Path scenarioFilePath,
            Path reportOutputDirectory,
            String reportFileName,
            Path screenshotOutputDirectory
    ) {
        this.scenarioFilePath = scenarioFilePath;
        this.reportOutputDirectory = reportOutputDirectory;
        this.reportFileName = reportFileName;
        this.reportFilePath = reportOutputDirectory.resolve(reportFileName).toAbsolutePath().normalize();
        this.screenshotOutputDirectory = screenshotOutputDirectory;
    }

    public static ExcelExecutionConfig load() {
        return fromProperties(loadDefaultProperties(), System::getProperty);
    }

    public static ExcelExecutionConfig load(Path propertiesPath) {
        return load(propertiesPath, Map.of());
    }

    public static ExcelExecutionConfig load(Path propertiesPath, Map<String, String> overrides) {
        return fromProperties(loadProperties(propertiesPath), key -> overrides == null ? null : overrides.get(key));
    }

    public static ExcelExecutionConfig fromProperties(Properties properties) {
        return fromProperties(properties, System::getProperty);
    }

    public static ExcelExecutionConfig fromProperties(Properties properties, Map<String, String> overrides) {
        return fromProperties(properties, key -> overrides == null ? null : overrides.get(key));
    }

    private static ExcelExecutionConfig fromProperties(Properties properties, PropertyResolver overrideResolver) {
        Properties safeProperties = properties == null ? new Properties() : properties;

        String scenarioFileValue = valueFor(safeProperties, overrideResolver, SCENARIO_FILE_PATH_KEY, "");
        if (scenarioFileValue.isBlank()) {
            throw new FrameworkException("Excel scenario file path is required. Please set excel.scenarioFilePath in excelConfig.properties.");
        }

        String reportOutputValue = valueFor(
                safeProperties,
                overrideResolver,
                REPORT_OUTPUT_DIRECTORY_KEY,
                DEFAULT_REPORT_OUTPUT_DIRECTORY
        );
        String reportFileName = normalizeReportFileName(valueFor(
                safeProperties,
                overrideResolver,
                REPORT_FILE_NAME_KEY,
                DEFAULT_REPORT_FILE_NAME
        ));
        Path reportOutputDirectory = resolvePath(reportOutputValue);

        String screenshotOutputValue = valueFor(
                safeProperties,
                overrideResolver,
                SCREENSHOT_OUTPUT_DIRECTORY_KEY,
                ""
        );
        Path screenshotOutputDirectory = screenshotOutputValue.isBlank()
                ? reportOutputDirectory.resolve("screenshots").toAbsolutePath().normalize()
                : resolvePath(screenshotOutputValue);

        return new ExcelExecutionConfig(
                resolvePath(scenarioFileValue),
                reportOutputDirectory,
                reportFileName,
                screenshotOutputDirectory
        );
    }

    public void validate() {
        validateScenarioFile();
        createDirectory(reportOutputDirectory, "Report output path");
        createDirectory(screenshotOutputDirectory, "Screenshot output path");

        LOGGER.info("Excel scenario file: {}", scenarioFilePath);
        LOGGER.info("Report output: {}", reportFilePath);
        LOGGER.info("Screenshot output: {}", screenshotOutputDirectory);
    }

    public Path getScenarioFilePath() {
        return scenarioFilePath;
    }

    public Path getReportOutputDirectory() {
        return reportOutputDirectory;
    }

    public String getReportFileName() {
        return reportFileName;
    }

    public Path getReportFilePath() {
        return reportFilePath;
    }

    public Path getScreenshotOutputDirectory() {
        return screenshotOutputDirectory;
    }

    private void validateScenarioFile() {
        if (!Files.exists(scenarioFilePath)) {
            throw new FrameworkException("Excel scenario file not found: " + displayPath(scenarioFilePath) + ".");
        }
        if (!Files.isRegularFile(scenarioFilePath)) {
            throw new FrameworkException("Excel scenario path is not a file: " + displayPath(scenarioFilePath) + ".");
        }
        if (!scenarioFilePath.getFileName().toString().toLowerCase().endsWith(".xlsx")) {
            throw new FrameworkException("Excel scenario file must be .xlsx: " + displayPath(scenarioFilePath) + ".");
        }
    }

    private static void createDirectory(Path directory, String label) {
        if (Files.exists(directory) && !Files.isDirectory(directory)) {
            throw new FrameworkException(label + " is not a directory: " + displayPath(directory) + ".");
        }

        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new FrameworkException("Unable to create " + label.toLowerCase() + ": " + displayPath(directory) + ".", exception);
        }
    }

    private static Properties loadDefaultProperties() {
        try (InputStream inputStream = openDefaultConfigStream()) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (IOException exception) {
            throw new FrameworkException("Unable to load Excel execution configuration from " + CONFIG_FILE_NAME + ".", exception);
        }
    }

    private static InputStream openDefaultConfigStream() throws IOException {
        InputStream classpathConfig = ExcelExecutionConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME);
        if (classpathConfig != null) {
            return classpathConfig;
        }

        Path configPath = Path.of("src", "test", "resources", CONFIG_FILE_NAME);
        if (Files.exists(configPath)) {
            return Files.newInputStream(configPath);
        }

        throw new IOException(CONFIG_FILE_NAME + " was not found on the classpath or at " + configPath.toAbsolutePath());
    }

    private static Properties loadProperties(Path propertiesPath) {
        try (InputStream inputStream = Files.newInputStream(propertiesPath)) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (IOException exception) {
            throw new FrameworkException("Unable to load Excel execution configuration from " + displayPath(propertiesPath) + ".", exception);
        }
    }

    private static String valueFor(
            Properties properties,
            PropertyResolver overrideResolver,
            String key,
            String defaultValue
    ) {
        String overrideValue = overrideResolver == null ? null : overrideResolver.get(key);
        if (overrideValue != null && !clean(overrideValue).isBlank()) {
            return clean(overrideValue);
        }

        String propertyValue = properties.getProperty(key);
        if (propertyValue != null && !clean(propertyValue).isBlank()) {
            return clean(propertyValue);
        }

        return defaultValue == null ? "" : defaultValue;
    }

    private static String normalizeReportFileName(String fileName) {
        String normalizedFileName = clean(fileName).isBlank() ? DEFAULT_REPORT_FILE_NAME : clean(fileName);
        return normalizedFileName.toLowerCase().endsWith(".html")
                ? normalizedFileName
                : normalizedFileName + ".html";
    }

    private static Path resolvePath(String value) {
        return Path.of(clean(value)).toAbsolutePath().normalize();
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

    private interface PropertyResolver {
        String get(String key);
    }
}
