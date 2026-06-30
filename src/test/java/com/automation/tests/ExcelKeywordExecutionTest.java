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
import com.automation.tests.support.ExcelKeywordTestWorkbookFactory;
import com.automation.tests.support.FakeWebDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ExcelKeywordExecutionTest {

    private static final Path TEMP_DIR = Path.of("target", "excel-keyword-execution-test");
    private static final Path LOCAL_HTML = Path.of("src", "test", "resources", "test-pages", "excel-keyword-test.html");
    private Path workbookPath;
    private ExcelExecutionConfig executionConfig;
    private String baseUrl;

    @BeforeClass
    public void createWorkbook() throws IOException {
        baseUrl = LOCAL_HTML.toAbsolutePath().toUri().toString();
        workbookPath = ExcelKeywordTestWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("excel-keyword-execution-test.xlsx"),
                baseUrl
        );
        executionConfig = executionConfig(workbookPath);
        executionConfig.validate();
    }

    @Test
    public void activeScenarioShouldExecuteExcelKeywordFlow() {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(executionConfig.getScenarioFilePath().toString())) {
            ScenarioRunner scenarioRunner = scenarioRunner(excelReader, fakeDriver);

            List<ExecutionResult> results = scenarioRunner.runActiveScenarios();

            Assert.assertEquals(results.size(), 11);
            Assert.assertTrue(results.stream().allMatch(ExecutionResult::isSuccess), failureMessages(results));
            Assert.assertTrue(results.stream().allMatch(result -> "1".equals(result.getScenarioNo())));
            Assert.assertEquals(
                    results.stream().map(ExecutionResult::getKeywordName).toList(),
                    List.of("openUrl", "input", "input", "click", "verifyDisplayed", "input", "screenshot", "click", "screenshot", "verifyText", "screenshot")
            );
            Assert.assertEquals(results.get(0).getResolvedValue(), baseUrl);
            Assert.assertEquals(fakeDriver.getCurrentUrl(), baseUrl);
            Assert.assertEquals(fakeDriver.element("//input[@id='username']").getValue(), "brs_admin");
            Assert.assertEquals(fakeDriver.element("//input[@id='password']").getValue(), "brs123");
            Assert.assertEquals(fakeDriver.element("//input[@id='bookingTitle']").getValue(), "Weekly Meeting");
            Assert.assertTrue(fakeDriver.element("//button[contains(text(),'Meeting Room A')]").isClicked());
            Assert.assertEquals(results.get(7).getResolvedXPath(), "//button[contains(text(),'Meeting Room A')]");
        }
    }

    private ScenarioRunner scenarioRunner(ExcelReader excelReader, FakeWebDriver fakeDriver) {
        ScenarioReader scenarioReader = new ScenarioReader(excelReader);
        StepReader stepReader = new StepReader(excelReader);
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        KeywordResolver keywordResolver = new KeywordResolver(fakeDriver.driver());
        KeywordEngine keywordEngine = new KeywordEngine(dataReader, objectRepositoryReader, keywordResolver, null, executionConfig);
        return new ScenarioRunner(scenarioReader, stepReader, keywordEngine);
    }

    private ExcelExecutionConfig executionConfig(Path scenarioFilePath) {
        Properties properties = new Properties();
        properties.setProperty(ExcelExecutionConfig.SCENARIO_FILE_PATH_KEY, scenarioFilePath.toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY, TEMP_DIR.resolve("reports").toString());
        return ExcelExecutionConfig.fromProperties(properties, Map.of());
    }

    private String failureMessages(List<ExecutionResult> results) {
        return results.stream()
                .filter(result -> !result.isSuccess())
                .map(ExecutionResult::getMessage)
                .toList()
                .toString();
    }

    private FakeWebDriver localPageDriver() {
        FakeWebDriver fakeDriver = new FakeWebDriver();
        fakeDriver.setTitle("Excel Keyword Test");
        fakeDriver.addElement("//input[@id='username']", "");
        fakeDriver.addElement("//input[@id='password']", "");
        fakeDriver.addElement("//button[@id='loginButton']", "Login");
        fakeDriver.addElement("//h1[@id='dashboard']", "Dashboard");
        fakeDriver.addElement("//input[@id='bookingTitle']", "");
        fakeDriver.addElement("//button[contains(text(),'Meeting Room A')]", "Meeting Room A");
        fakeDriver.addElement("//div[@id='message']", "Booking created successfully");
        return fakeDriver;
    }
}
