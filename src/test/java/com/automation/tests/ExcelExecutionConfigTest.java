package com.automation.tests;

import com.automation.config.ExcelExecutionConfig;
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

        Assert.assertTrue(config.getScenarioFilePath().endsWith(Path.of("src", "test", "resources", "testdata", "Template Testing.xlsx")));
        Assert.assertEquals(config.getReportFileName(), "ExcelAutomationReport.html");
        Assert.assertTrue(config.getReportFilePath().endsWith(Path.of("test-output", "reports", "ExcelAutomationReport.html")));
        Assert.assertTrue(config.getScreenshotOutputDirectory().endsWith(Path.of("test-output", "screenshots")));
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
    public void screenshotDirectoryShouldDefaultToReportDirectoryScreenshotsWhenBlank() throws IOException {
        Path scenarioFile = createFile("default-screenshot-dir.xlsx");
        Path reportDirectory = TEMP_DIR.resolve("report-with-default-screenshots");
        Properties properties = properties(scenarioFile.toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY, reportDirectory.toString());
        properties.setProperty(ExcelExecutionConfig.SCREENSHOT_OUTPUT_DIRECTORY_KEY, " ");

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties, Map.of());

        Assert.assertEquals(
                config.getScreenshotOutputDirectory(),
                reportDirectory.resolve("screenshots").toAbsolutePath().normalize()
        );
    }

    @Test
    public void reportFileNameShouldDefaultWhenBlank() throws IOException {
        Path scenarioFile = createFile("default-report-name.xlsx");
        Properties properties = properties(scenarioFile.toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_FILE_NAME_KEY, " ");

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties, Map.of());

        Assert.assertEquals(config.getReportFileName(), "ExcelAutomationReport.html");
    }

    @Test
    public void reportFileNameShouldAppendHtmlWhenMissingExtension() throws IOException {
        Path scenarioFile = createFile("append-report-extension.xlsx");
        Properties properties = properties(scenarioFile.toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_FILE_NAME_KEY, "BookingRoomReport");

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties, Map.of());

        Assert.assertEquals(config.getReportFileName(), "BookingRoomReport.html");
        Assert.assertTrue(config.getReportFilePath().endsWith(Path.of("BookingRoomReport.html")));
    }

    @Test
    public void reportOutputFileShouldFailClearly() throws IOException {
        Path scenarioFile = createFile("report-output-file.xlsx");
        Path reportPath = createFile("reports-as-file.html");
        Properties properties = properties(scenarioFile.toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY, reportPath.toString());

        ExcelExecutionConfig config = ExcelExecutionConfig.fromProperties(properties, Map.of());
        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, config::validate);

        Assert.assertTrue(exception.getMessage().contains("Report output path is not a directory:"));
    }

    private Properties properties(String scenarioFilePath) {
        Properties properties = new Properties();
        properties.setProperty(ExcelExecutionConfig.SCENARIO_FILE_PATH_KEY, scenarioFilePath);
        properties.setProperty(ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY, TEMP_DIR.resolve("reports").toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_FILE_NAME_KEY, "ExcelAutomationReport.html");
        properties.setProperty(ExcelExecutionConfig.SCREENSHOT_OUTPUT_DIRECTORY_KEY, TEMP_DIR.resolve("screenshots").toString());
        return properties;
    }

    private Path createFile(String fileName) throws IOException {
        Path path = TEMP_DIR.resolve(fileName);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "test");
        return path;
    }
}
