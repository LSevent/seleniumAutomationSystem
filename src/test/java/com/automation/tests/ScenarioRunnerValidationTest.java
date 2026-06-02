package com.automation.tests;

import com.automation.engine.FunctionResolver;
import com.automation.engine.KeywordEngine;
import com.automation.engine.ScenarioRunner;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.models.Scenario;
import com.automation.tests.support.FakeWebDriver;
import com.automation.tests.support.ValidationWorkbookFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;

import static com.automation.tests.support.ValidationWorkbookFactory.scenarioSheet;

public class ScenarioRunnerValidationTest {

    private static final Path TEMP_DIR = Path.of("target", "phase-11-runner-validation");

    @Test
    public void activeScenarioWithoutActiveTestcaseShouldFailClearly() throws IOException {
        Path workbookPath = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("no-active-testcase.xlsx"),
                ValidationWorkbookFactory.scenarios(new Object[][]{{1, "Y", "Local Keyword Test", "Active scenario"}}),
                scenarioSheet("Local Keyword Test", new Object[][]{{"Inactive Login", "N", "", "", "", "", "Inactive only"}}),
                ValidationWorkbookFactory.objectRepository(new Object[][]{{"BRS", "btnLogin", "//button[@id='login']", "Login"}})
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                ScenarioRunner runner = runner(excelReader);
                Scenario scenario = new ScenarioReader(excelReader).getActiveScenarios().get(0);
                runner.runScenario(scenario);
            }
        });

        Assert.assertEquals(exception.getMessage(), "Active scenario has no active testcase. Scenario NO = 1, ACTION = Local Keyword Test.");
    }

    private ScenarioRunner runner(ExcelReader excelReader) {
        FakeWebDriver fakeWebDriver = new FakeWebDriver();
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        FunctionResolver functionResolver = new FunctionResolver(fakeWebDriver.driver());
        KeywordEngine keywordEngine = new KeywordEngine(dataReader, objectRepositoryReader, functionResolver);
        return new ScenarioRunner(new ScenarioReader(excelReader), new StepReader(excelReader), keywordEngine);
    }
}
