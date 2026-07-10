package com.automation.tests;

import com.automation.config.ExcelExecutionConfig;
import com.automation.reports.ExcelReportConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public class ExcelExecutionConfigTest {

    private static final Path TEMP_DIR = Path.of("target", "excel-execution-config-test");

    @Test
    public void shouldLoadDefaultExcelConfigProperties() {
        ExcelExecutionConfig config = ExcelExecutionConfig.load(
                Path.of("src", "test", "resources", "excelConfig.properties"),
                Map.of()
        );

        Assert.assertEquals(config.getScenarioFilePath(), Path.of("C:/Automation/BRS/Booking Room System.xlsx").toAbsolutePath().normalize());
        Assert.assertEquals(config.getReportRootDirectory(), Path.of("C:/Automation/BRS/Reports").toAbsolutePath().normalize());
        assertRunFolder(config);
        Assert.assertEquals(config.getReportFileName(), "Report-Booking Room System.html");
        Assert.assertEquals(
                config.getReportFilePath(),
                config.getReportOutputDirectory().resolve("Report-Booking Room System.html").toAbsolutePath().normalize()
        );
        Assert.assertEquals(
                config.getScreenshotOutputDirectory(),
                config.getReportOutputDirectory().resolve("Screenshots").toAbsolutePath().normalize()
        );
        Assert.assertEquals(config.getBrowser(), "chrome");
        Assert.assertFalse(config.isHeadless());
        Assert.assertEquals(config.getTimeoutSeconds(), 10);
        Assert.assertFalse(config.isRemote());
        Assert.assertEquals(config.getGridUrl(), "http://localhost:4444/wd/hub");
        Assert.assertFalse(config.isShowSensitiveData());
        Assert.assertTrue(config.isScreenshotOnFailure());
        Assert.assertTrue(config.isManualScreenshotEnabled());
    }

    @Test
    public void shouldSupportRelativeScenarioPath() throws IOException {
        Path scenarioFile = createFile("relative-scenario.xlsx");

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties(scenarioFile.toString()), Map.of());

        Assert.assertEquals(config.getScenarioFilePath(), scenarioFile.toAbsolutePath().normalize());
    }

    @Test
    public void shouldSupportAbsoluteScenarioPath() throws IOException {
        Path scenarioFile = createFile("absolute-scenario.xlsx").toAbsolutePath().normalize();

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties(scenarioFile.toString()), Map.of());

        Assert.assertEquals(config.getScenarioFilePath(), scenarioFile);
    }

    @Test
    public void systemPropertyOverridesShouldWinOverPropertiesFileValues() throws IOException {
        Path propertyScenario = createFile("property-scenario.xlsx");
        Path overrideScenario = createFile("override-scenario.xlsx");
        Properties properties = properties(propertyScenario.toString());

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(
                properties,
                Map.of(ExcelExecutionConfig.SCENARIO_FILE_PATH_KEY, overrideScenario.toString())
        );

        Assert.assertEquals(config.getScenarioFilePath(), overrideScenario.toAbsolutePath().normalize());
    }

    @Test
    public void systemPropertyOverridesShouldApplyToExcelRunnerSettings() throws IOException {
        Path scenarioFile = createFile("excel-runner-overrides.xlsx");
        Properties properties = properties(scenarioFile.toString());

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(
                properties,
                Map.of(
                        ExcelExecutionConfig.BROWSER_KEY, "firefox",
                        ExcelExecutionConfig.HEADLESS_KEY, "true",
                        ExcelExecutionConfig.TIMEOUT_KEY, "25",
                        ExcelExecutionConfig.REMOTE_KEY, "true",
                        ExcelExecutionConfig.GRID_URL_KEY, "http://grid.example/wd/hub",
                        ExcelExecutionConfig.REPORT_SHOW_SENSITIVE_DATA_KEY, "true",
                        ExcelExecutionConfig.REPORT_SCREENSHOT_ON_FAILURE_KEY, "false",
                        ExcelExecutionConfig.REPORT_MANUAL_SCREENSHOT_ENABLED_KEY, "false"
                )
        );

        Assert.assertEquals(config.getBrowser(), "firefox");
        Assert.assertTrue(config.isHeadless());
        Assert.assertEquals(config.getTimeoutSeconds(), 25);
        Assert.assertTrue(config.isRemote());
        Assert.assertEquals(config.getGridUrl(), "http://grid.example/wd/hub");
        Assert.assertTrue(config.isShowSensitiveData());
        Assert.assertFalse(config.isScreenshotOnFailure());
        Assert.assertFalse(config.isManualScreenshotEnabled());
    }

    @Test
    public void excelReportConfigShouldUseExcelExecutionConfigValues() throws IOException {
        Path scenarioFile = createFile("excel-report-config.xlsx");
        Properties properties = properties(scenarioFile.toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_SHOW_SENSITIVE_DATA_KEY, "true");
        properties.setProperty(ExcelExecutionConfig.REPORT_SCREENSHOT_ON_FAILURE_KEY, "false");
        properties.setProperty(ExcelExecutionConfig.REPORT_MANUAL_SCREENSHOT_ENABLED_KEY, "false");

        ExcelExecutionConfig executionConfig = ExcelExecutionConfig.fromProperties(properties, Map.of());
        ExcelReportConfig reportConfig = ExcelReportConfig.fromExcelExecutionConfig(executionConfig);

        Assert.assertTrue(reportConfig.isShowSensitiveData());
        Assert.assertFalse(reportConfig.isScreenshotOnFailure());
        Assert.assertFalse(reportConfig.isManualScreenshotEnabled());
    }

    @Test
    public void missingScenarioFilePathShouldFailClearly() {
        Properties properties = properties("");

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> ExcelExecutionConfig.fromProperties(properties, Map.of())
        );

        Assert.assertEquals(
                exception.getMessage(),
                "Excel scenario file path is required. Please set excel.scenarioFilePath in excelConfig.properties."
        );
    }

    @Test
    public void nonExistingScenarioFileShouldFailClearly() {
        Path scenarioFile = TEMP_DIR.resolve("missing.xlsx");
        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties(scenarioFile.toString()), Map.of());

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, config::validate);

        Assert.assertTrue(exception.getMessage().contains("Excel scenario file not found:"));
        Assert.assertTrue(exception.getMessage().contains("missing.xlsx"));
    }

    @Test
    public void invalidExcelRunnerTimeoutShouldFailClearly() throws IOException {
        Path scenarioFile = createFile("invalid-timeout.xlsx");
        Properties properties = properties(scenarioFile.toString());
        properties.setProperty(ExcelExecutionConfig.TIMEOUT_KEY, "0");

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> ExcelExecutionConfig.fromProperties(properties, Map.of())
        );

        Assert.assertTrue(exception.getMessage().contains("Excel execution configuration key 'timeout' must be a positive number."));
    }

    @Test
    public void scenarioPathPointingToDirectoryShouldFailClearly() throws IOException {
        Path scenarioDirectory = TEMP_DIR.resolve("scenario-directory");
        Files.createDirectories(scenarioDirectory);
        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties(scenarioDirectory.toString()), Map.of());

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, config::validate);

        Assert.assertTrue(exception.getMessage().contains("Excel scenario path is not a file:"));
        Assert.assertTrue(exception.getMessage().contains("scenario-directory"));
    }

    @Test
    public void nonXlsxScenarioFileShouldFailClearly() throws IOException {
        Path scenarioFile = createFile("scenario.csv");
        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties(scenarioFile.toString()), Map.of());

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, config::validate);

        Assert.assertTrue(exception.getMessage().contains("Excel scenario file must be .xlsx:"));
        Assert.assertTrue(exception.getMessage().contains("scenario.csv"));
    }

    @Test
    public void reportDirectoryShouldBeCreatedWhenMissing() throws IOException {
        Path scenarioFile = createFile("report-directory-created.xlsx");
        Path reportDirectory = TEMP_DIR.resolve("new-report-dir");
        Properties properties = properties(scenarioFile.toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY, reportDirectory.toString());

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties, Map.of());
        config.validate();

        Assert.assertTrue(Files.isDirectory(reportDirectory));
    }

    @Test
    public void screenshotDirectoryShouldBeDerivedFromReportDirectory() throws IOException {
        Path scenarioFile = createFile("default-screenshot-dir.xlsx");
        Path reportDirectory = TEMP_DIR.resolve("report-with-default-screenshots");
        Properties properties = properties(scenarioFile.toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY, reportDirectory.toString());

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties, Map.of());

        Assert.assertEquals(
                config.getScreenshotOutputDirectory(),
                config.getReportOutputDirectory().resolve("Screenshots").toAbsolutePath().normalize()
        );
        Assert.assertEquals(config.getReportRootDirectory(), reportDirectory.toAbsolutePath().normalize());
        assertRunFolder(config);
    }

    @Test
    public void changingReportDirectoryShouldChangeScreenshotDirectory() throws IOException {
        Path scenarioFile = createFile("changed-report-directory.xlsx");
        Path reportDirectory = TEMP_DIR.resolve("changed-report-dir");
        Properties properties = properties(scenarioFile.toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY, reportDirectory.toString());

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties, Map.of());

        Assert.assertEquals(
                config.getScreenshotOutputDirectory(),
                config.getReportOutputDirectory().resolve("Screenshots").toAbsolutePath().normalize()
        );
        Assert.assertEquals(config.getReportRootDirectory(), reportDirectory.toAbsolutePath().normalize());
        assertRunFolder(config);
    }

    @Test
    public void reportFileNameShouldBeDerivedFromScenarioFileName() throws IOException {
        Path scenarioFile = createFile("Booking Room System.xlsx");
        Properties properties = properties(scenarioFile.toString());

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties, Map.of());

        Assert.assertEquals(config.getReportFileName(), "Report-Booking Room System.html");
        Assert.assertTrue(config.getReportFilePath().endsWith(Path.of("Report-Booking Room System.html")));
    }

    @Test
    public void reportFileNameShouldRemoveFinalExtensionOnly() {
        Properties properties = properties("C:/Automation/BRS/LoginScenarios.xlsm");

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties, Map.of());

        Assert.assertEquals(config.getReportFileName(), "Report-LoginScenarios.html");
    }

    @Test
    public void reportFileNameShouldSupportWindowsUnixAndRelativePaths() {
        Assert.assertEquals(
                ExcelExecutionConfig.fromProperties(properties("C:\\Automation\\BRS\\Booking Room System.xlsx"), Map.of()).getReportFileName(),
                "Report-Booking Room System.html"
        );
        Assert.assertEquals(
                ExcelExecutionConfig.fromProperties(properties("/automation/brs/Regression_Test.xlsx"), Map.of()).getReportFileName(),
                "Report-Regression_Test.html"
        );
    }

    @Test
    public void legacyReportFileNamePropertyShouldBeIgnored() throws IOException {
        Path scenarioFile = createFile("ignored-report-name.xlsx");
        Properties properties = properties(scenarioFile.toString());
        properties.setProperty("report.fileName", "ManualReport.html");

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties, Map.of());

        Assert.assertEquals(config.getReportFileName(), "Report-ignored-report-name.html");
    }

    @Test
    public void legacyScreenshotDirectoryPropertyShouldBeIgnored() throws IOException {
        Path scenarioFile = createFile("ignored-screenshot-dir.xlsx");
        Path reportDirectory = TEMP_DIR.resolve("report-dir-for-ignored-screenshots");
        Properties properties = properties(scenarioFile.toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY, reportDirectory.toString());
        properties.setProperty("screenshot.outputDirectory", TEMP_DIR.resolve("manual-screenshots").toString());

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties, Map.of());

        Assert.assertEquals(
                config.getScreenshotOutputDirectory(),
                config.getReportOutputDirectory().resolve("Screenshots").toAbsolutePath().normalize()
        );
        Assert.assertEquals(config.getReportRootDirectory(), reportDirectory.toAbsolutePath().normalize());
        assertRunFolder(config);
    }

    @Test
    public void legacyReportFileNameSystemPropertyShouldBeIgnored() throws IOException {
        Path scenarioFile = createFile("ignored-report-override.xlsx");
        Properties properties = properties(scenarioFile.toString());

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(
                properties,
                Map.of("report.fileName", "ManualReport.html")
        );

        Assert.assertEquals(config.getReportFileName(), "Report-ignored-report-override.html");
    }

    @Test
    public void legacyScreenshotDirectorySystemPropertyShouldBeIgnored() throws IOException {
        Path scenarioFile = createFile("ignored-screenshot-override.xlsx");
        Path reportDirectory = TEMP_DIR.resolve("report-dir-for-ignored-screenshot-override");
        Properties properties = properties(scenarioFile.toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY, reportDirectory.toString());

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(
                properties,
                Map.of("screenshot.outputDirectory", TEMP_DIR.resolve("manual-screenshots").toString())
        );

        Assert.assertEquals(
                config.getScreenshotOutputDirectory(),
                config.getReportOutputDirectory().resolve("Screenshots").toAbsolutePath().normalize()
        );
        Assert.assertEquals(config.getReportRootDirectory(), reportDirectory.toAbsolutePath().normalize());
        assertRunFolder(config);
    }

    @Test
    public void reportOutputFileShouldFailClearly() throws IOException {
        Path scenarioFile = createFile("report-output-file.xlsx");
        Path reportPath = createFile("reports-as-file.html");
        Properties properties = properties(scenarioFile.toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY, reportPath.toString());

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties, Map.of());
        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, config::validate);

        Assert.assertTrue(exception.getMessage().contains("Report root output path is not a directory:"));
    }

    private static void assertRunFolder(ExcelExecutionConfig config) {
        Assert.assertTrue(
                config.getReportRunFolderName().matches("\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}-\\d{3}"),
                "Run folder should be timestamp only. Actual: " + config.getReportRunFolderName()
        );
        Assert.assertEquals(
                config.getReportOutputDirectory(),
                config.getReportRootDirectory().resolve(config.getReportRunFolderName()).toAbsolutePath().normalize()
        );
    }

    private Properties properties(String scenarioFilePath) {
        Properties properties = new Properties();
        properties.setProperty(ExcelExecutionConfig.SCENARIO_FILE_PATH_KEY, scenarioFilePath);
        properties.setProperty(ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY, TEMP_DIR.resolve("reports").toString());
        return properties;
    }

    private Path createFile(String fileName) throws IOException {
        Path path = TEMP_DIR.resolve(fileName);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "test");
        return path;
    }
}
