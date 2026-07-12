package com.automation.config;

import com.automation.exceptions.FrameworkException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;

public class ExcelExecutionConfig {

    public static final String CONFIG_FILE_NAME = "excelConfig.properties";
    public static final String SCENARIO_FILE_PATH_KEY = "excel.scenarioFilePath";
    public static final String REPORT_OUTPUT_DIRECTORY_KEY = "report.outputDirectory";
    public static final String DEFAULT_REPORT_OUTPUT_DIRECTORY = "test-output/reports";
    public static final String BROWSER_KEY = "browser";
    public static final String HEADLESS_KEY = "headless";
    public static final String TIMEOUT_KEY = "timeout";
    public static final String REMOTE_KEY = "remote";
    public static final String GRID_URL_KEY = "gridUrl";
    public static final String REPORT_SHOW_SENSITIVE_DATA_KEY = "report.showSensitiveData";
    public static final String REPORT_SCREENSHOT_ON_FAILURE_KEY = "report.screenshotOnFailure";
    public static final String REPORT_MANUAL_SCREENSHOT_ENABLED_KEY = "report.manualScreenshotEnabled";

    public static final String DEFAULT_BROWSER = "chrome";
    public static final boolean DEFAULT_HEADLESS = false;
    public static final int DEFAULT_TIMEOUT_SECONDS = 10;
    public static final boolean DEFAULT_REMOTE = false;
    public static final String DEFAULT_GRID_URL = "http://localhost:4444/wd/hub";
    public static final boolean DEFAULT_SHOW_SENSITIVE_DATA = false;
    public static final boolean DEFAULT_SCREENSHOT_ON_FAILURE = true;
    public static final boolean DEFAULT_MANUAL_SCREENSHOT_ENABLED = true;

    private static final Logger LOGGER = LogManager.getLogger(ExcelExecutionConfig.class);
    private static final DateTimeFormatter RUN_FOLDER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");

    private final Path scenarioFilePath;
    private final Path reportRootDirectory;
    private final String reportRunFolderName;
    private final Path reportOutputDirectory;
    private final String reportFileName;
    private final Path reportFilePath;
    private final Path screenshotOutputDirectory;
    private final String browser;
    private final boolean headless;
    private final int timeoutSeconds;
    private final boolean remote;
    private final String gridUrl;
    private final boolean showSensitiveData;
    private final boolean screenshotOnFailure;
    private final boolean manualScreenshotEnabled;

    private ExcelExecutionConfig(
            Path scenarioFilePath,
            Path reportRootDirectory,
            String reportRunFolderName,
            Path reportOutputDirectory,
            String reportFileName,
            Path screenshotOutputDirectory,
            String browser,
            boolean headless,
            int timeoutSeconds,
            boolean remote,
            String gridUrl,
            boolean showSensitiveData,
            boolean screenshotOnFailure,
            boolean manualScreenshotEnabled
    ) {
        this.scenarioFilePath = scenarioFilePath;
        this.reportRootDirectory = reportRootDirectory;
        this.reportRunFolderName = reportRunFolderName;
        this.reportOutputDirectory = reportOutputDirectory;
        this.reportFileName = reportFileName;
        this.reportFilePath = reportOutputDirectory.resolve(reportFileName).toAbsolutePath().normalize();
        this.screenshotOutputDirectory = screenshotOutputDirectory;
        this.browser = browser;
        this.headless = headless;
        this.timeoutSeconds = timeoutSeconds;
        this.remote = remote;
        this.gridUrl = gridUrl;
        this.showSensitiveData = showSensitiveData;
        this.screenshotOnFailure = screenshotOnFailure;
        this.manualScreenshotEnabled = manualScreenshotEnabled;
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
        String reportFileName = deriveReportFileName(scenarioFileValue);
        Path reportRootDirectory = resolvePath(reportOutputValue);
        String reportRunFolderName = deriveReportRunFolderName();
        Path reportOutputDirectory = reportRootDirectory.resolve(reportRunFolderName).toAbsolutePath().normalize();
        Path screenshotOutputDirectory = reportOutputDirectory.resolve("Screenshots").toAbsolutePath().normalize();
        String browser = valueFor(safeProperties, overrideResolver, BROWSER_KEY, DEFAULT_BROWSER);
        boolean headless = booleanValueFor(safeProperties, overrideResolver, HEADLESS_KEY, DEFAULT_HEADLESS);
        int timeoutSeconds = intValueFor(safeProperties, overrideResolver, TIMEOUT_KEY, DEFAULT_TIMEOUT_SECONDS);
        boolean remote = booleanValueFor(safeProperties, overrideResolver, REMOTE_KEY, DEFAULT_REMOTE);
        String gridUrl = valueFor(safeProperties, overrideResolver, GRID_URL_KEY, DEFAULT_GRID_URL);
        boolean showSensitiveData = booleanValueFor(
                safeProperties,
                overrideResolver,
                REPORT_SHOW_SENSITIVE_DATA_KEY,
                DEFAULT_SHOW_SENSITIVE_DATA
        );
        boolean screenshotOnFailure = booleanValueFor(
                safeProperties,
                overrideResolver,
                REPORT_SCREENSHOT_ON_FAILURE_KEY,
                DEFAULT_SCREENSHOT_ON_FAILURE
        );
        boolean manualScreenshotEnabled = booleanValueFor(
                safeProperties,
                overrideResolver,
                REPORT_MANUAL_SCREENSHOT_ENABLED_KEY,
                DEFAULT_MANUAL_SCREENSHOT_ENABLED
        );

        return new ExcelExecutionConfig(
                resolvePath(scenarioFileValue),
                reportRootDirectory,
                reportRunFolderName,
                reportOutputDirectory,
                reportFileName,
                screenshotOutputDirectory,
                browser,
                headless,
                timeoutSeconds,
                remote,
                gridUrl,
                showSensitiveData,
                screenshotOnFailure,
                manualScreenshotEnabled
        );
    }

    public void validate() {
        validateScenarioFile();
        createDirectory(reportRootDirectory, "Report root output path");
        createDirectory(reportOutputDirectory, "Report run output path");
        createDirectory(screenshotOutputDirectory, "Screenshot output path");

        LOGGER.info("Excel run configured. Workbook = {} | Report = {}", scenarioFilePath, reportFilePath);
        LOGGER.info(
                "Browser = {} | Headless = {} | Remote = {} | Screenshots = {}",
                browser,
                headless,
                remote,
                screenshotOutputDirectory
        );
        LOGGER.debug("Report root = {} | Run folder = {}", reportRootDirectory, reportRunFolderName);
    }

    public Path getScenarioFilePath() {
        return scenarioFilePath;
    }

    public Path getReportRootDirectory() {
        return reportRootDirectory;
    }

    public String getReportRunFolderName() {
        return reportRunFolderName;
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

    public String getBrowser() {
        return browser;
    }

    public boolean isHeadless() {
        return headless;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public boolean isRemote() {
        return remote;
    }

    public String getGridUrl() {
        return gridUrl;
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

    private static boolean booleanValueFor(
            Properties properties,
            PropertyResolver overrideResolver,
            String key,
            boolean defaultValue
    ) {
        return Boolean.parseBoolean(valueFor(properties, overrideResolver, key, String.valueOf(defaultValue)));
    }

    private static int intValueFor(
            Properties properties,
            PropertyResolver overrideResolver,
            String key,
            int defaultValue
    ) {
        String value = valueFor(properties, overrideResolver, key, String.valueOf(defaultValue));
        try {
            int parsedValue = Integer.parseInt(value);
            if (parsedValue <= 0) {
                throw new NumberFormatException("Value must be greater than zero.");
            }
            return parsedValue;
        } catch (NumberFormatException exception) {
            throw new FrameworkException("Excel execution configuration key '" + key
                    + "' must be a positive number. Actual value: " + value, exception);
        }
    }

    private static String deriveReportFileName(String scenarioFilePath) {
        String baseFileName = extractBaseFileName(scenarioFilePath);
        if (baseFileName.isBlank()) {
            throw new FrameworkException("Excel scenario file name is required to derive the report file name.");
        }
        return "Report-" + baseFileName + ".html";
    }

    private static String deriveReportRunFolderName() {
        return LocalDateTime.now().format(RUN_FOLDER_TIME_FORMAT);
    }

    private static String extractBaseFileName(String scenarioFilePath) {
        String cleanedPath = clean(scenarioFilePath);
        int lastForwardSlash = cleanedPath.lastIndexOf('/');
        int lastBackwardSlash = cleanedPath.lastIndexOf('\\');
        int lastSeparator = Math.max(lastForwardSlash, lastBackwardSlash);
        String fileName = lastSeparator >= 0 ? cleanedPath.substring(lastSeparator + 1) : cleanedPath;
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
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
