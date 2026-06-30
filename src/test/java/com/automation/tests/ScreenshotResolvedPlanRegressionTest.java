package com.automation.tests;

import com.automation.config.ExcelExecutionConfig;
import com.automation.engine.KeywordResolver;
import com.automation.engine.KeywordEngine;
import com.automation.engine.ScenarioRunner;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.models.ExecutionResult;
import com.automation.reports.ExcelExecutionReporter;
import com.automation.reports.ExcelReportConfig;
import com.automation.tests.support.ExcelKeywordTestWorkbookFactory;
import com.automation.tests.support.FakeWebDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Test(singleThreaded = true)
public class ScreenshotResolvedPlanRegressionTest {

    private static final Path TEMP_DIR = Path.of("target", "phase-13c4-screenshot-regression");
    private static final Path LOCAL_HTML = Path.of(
            "src", "test", "resources", "test-pages", "excel-keyword-test.html"
    );

    private String baseUrl;

    @BeforeClass
    public void prepare() {
        baseUrl = LOCAL_HTML.toAbsolutePath().toUri().toString();
    }

    @Test
    public void manualScreenshotsShouldProduceEvidenceAndGalleryFromResolvedPlan() throws IOException {
        Path workbook = ExcelKeywordTestWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("manual-screenshots.xlsx"),
                baseUrl
        );
        ExcelExecutionConfig executionConfig = executionConfig(workbook, "manual");

        List<ExecutionResult> results = run(
                workbook,
                executionConfig,
                new ExcelReportConfig(false, true, true)
        );

        List<ExecutionResult> screenshots = results.stream()
                .filter(result -> "screenshot".equalsIgnoreCase(result.getKeywordName()))
                .toList();
        Assert.assertEquals(screenshots.size(), 3);
        for (ExecutionResult screenshot : screenshots) {
            Assert.assertEquals(screenshot.getExecutionSource(), "REPORT");
            Assert.assertTrue(screenshot.getEvidence().toLowerCase().endsWith(".png"));
            Assert.assertTrue(Files.exists(Path.of(screenshot.getEvidence())));
        }

        String reportHtml = Files.readString(executionConfig.getReportFilePath());
        Assert.assertTrue(reportHtml.contains("Evidence Gallery"));
        Assert.assertTrue(reportHtml.contains("Manual screenshot"));
        Assert.assertTrue(reportHtml.contains("After input title"));
        Assert.assertTrue(reportHtml.contains("After select room"));
        Assert.assertTrue(reportHtml.contains("After submit"));
    }

    @Test
    public void failureScreenshotShouldAppearInGalleryAndFailureDetails() throws IOException {
        Path workbook = ExcelKeywordTestWorkbookFactory.createFailureWorkbook(
                TEMP_DIR.resolve("failure-screenshot.xlsx"),
                baseUrl,
                94,
                "Resolved Failure Flow",
                "Resolved-plan failure screenshot",
                "Failure Screenshot Testcase"
        );
        ExcelExecutionConfig executionConfig = executionConfig(workbook, "failure");

        List<ExecutionResult> results = run(
                workbook,
                executionConfig,
                new ExcelReportConfig(false, true, true)
        );

        Assert.assertFalse(results.get(results.size() - 1).isSuccess());
        try (var files = Files.list(executionConfig.getScreenshotOutputDirectory())) {
            Assert.assertTrue(files.anyMatch(path -> path.getFileName().toString().toLowerCase().endsWith(".png")));
        }

        String reportHtml = Files.readString(executionConfig.getReportFilePath());
        Assert.assertTrue(reportHtml.contains("Failure screenshot"));
        Assert.assertTrue(reportHtml.contains("Evidence Gallery"));
        Assert.assertTrue(reportHtml.contains("Failure Details"));
        Assert.assertTrue(reportHtml.contains("Error Message"));
        Assert.assertTrue(reportHtml.contains("Screenshots/"));
    }

    private List<ExecutionResult> run(
            Path workbook,
            ExcelExecutionConfig executionConfig,
            ExcelReportConfig reportConfig
    ) {
        FakeWebDriver driver = driver();
        try (ExcelReader excelReader = new ExcelReader(workbook.toString())) {
            DataReader dataReader = new DataReader(excelReader);
            ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
            KeywordEngine keywordEngine = new KeywordEngine(
                    dataReader,
                    objectRepositoryReader,
                    new KeywordResolver(driver.driver()),
                    reportConfig,
                    executionConfig
            );
            ExcelExecutionReporter reporter = new ExcelExecutionReporter(
                    driver.driver(),
                    reportConfig,
                    executionConfig
            );
            ScenarioRunner runner = new ScenarioRunner(
                    new ScenarioReader(excelReader),
                    new StepReader(excelReader),
                    keywordEngine,
                    reporter
            );
            return runner.runActiveScenarios();
        }
    }

    private ExcelExecutionConfig executionConfig(Path workbook, String name) {
        Path reportDirectory = TEMP_DIR.resolve(name).resolve("reports");
        Properties properties = new Properties();
        properties.setProperty(ExcelExecutionConfig.SCENARIO_FILE_PATH_KEY, workbook.toString());
        properties.setProperty(
                ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY,
                reportDirectory.toString()
        );
        return ExcelExecutionConfig.fromProperties(properties, Map.of());
    }

    private FakeWebDriver driver() {
        FakeWebDriver driver = new FakeWebDriver();
        driver.setTitle("Excel Keyword Test");
        driver.addElement("//input[@id='username']", "");
        driver.addElement("//input[@id='password']", "");
        driver.addElement("//button[@id='loginButton']", "Login");
        driver.addElement("//h1[@id='dashboard']", "Dashboard");
        driver.addElement("//input[@id='bookingTitle']", "");
        driver.addElement("//button[contains(text(),'Meeting Room A')]", "Meeting Room A");
        driver.addElement("//div[@id='message']", "Booking created successfully");
        return driver;
    }
}
