package com.automation.tests;

import com.automation.engine.FunctionResolver;
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

public class ExcelKeywordExecutionTest {

    private static final Path TEMP_DIR = Path.of("target", "excel-keyword-execution-test");
    private static final Path LOCAL_HTML = Path.of("src", "test", "resources", "test-pages", "excel-keyword-test.html");
    private Path workbookPath;
    private String baseUrl;

    @BeforeClass
    public void createWorkbook() throws IOException {
        baseUrl = LOCAL_HTML.toAbsolutePath().toUri().toString();
        workbookPath = ExcelKeywordTestWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("excel-keyword-execution-test.xlsx"),
                baseUrl
        );
    }

    @Test
    public void activeScenarioShouldExecuteExcelKeywordFlow() {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ScenarioRunner scenarioRunner = scenarioRunner(excelReader, fakeDriver);

            List<ExecutionResult> results = scenarioRunner.runActiveScenarios();

            Assert.assertEquals(results.size(), 8);
            Assert.assertTrue(results.stream().allMatch(ExecutionResult::isSuccess), failureMessages(results));
            Assert.assertTrue(results.stream().allMatch(result -> "1".equals(result.getScenarioNo())));
            Assert.assertEquals(
                    results.stream().map(ExecutionResult::getFunctionName).toList(),
                    List.of("openUrl", "input", "input", "click", "verifyDisplayed", "input", "click", "verifyText")
            );
            Assert.assertEquals(results.get(0).getResolvedValue(), baseUrl);
            Assert.assertEquals(fakeDriver.getCurrentUrl(), baseUrl);
            Assert.assertEquals(fakeDriver.element("//input[@id='username']").getValue(), "brs_admin");
            Assert.assertEquals(fakeDriver.element("//input[@id='password']").getValue(), "brs123");
            Assert.assertEquals(fakeDriver.element("//input[@id='bookingTitle']").getValue(), "Weekly Meeting");
            Assert.assertTrue(fakeDriver.element("//button[contains(text(),'Meeting Room A')]").isClicked());
            Assert.assertEquals(results.get(6).getResolvedXpath(), "//button[contains(text(),'Meeting Room A')]");
        }
    }

    private ScenarioRunner scenarioRunner(ExcelReader excelReader, FakeWebDriver fakeDriver) {
        ScenarioReader scenarioReader = new ScenarioReader(excelReader);
        StepReader stepReader = new StepReader(excelReader);
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        FunctionResolver functionResolver = new FunctionResolver(fakeDriver.driver());
        KeywordEngine keywordEngine = new KeywordEngine(dataReader, objectRepositoryReader, functionResolver);
        return new ScenarioRunner(scenarioReader, stepReader, keywordEngine);
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
