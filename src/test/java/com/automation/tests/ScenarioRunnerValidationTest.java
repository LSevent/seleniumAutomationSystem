package com.automation.tests;

import com.automation.engine.KeywordResolver;
import com.automation.engine.KeywordEngine;
import com.automation.engine.ScenarioRunner;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.exceptions.FrameworkException;
import com.automation.tests.support.FakeWebDriver;
import com.automation.tests.support.ValidationWorkbookFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

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

        FrameworkException exception = Assert.expectThrows(FrameworkException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                ScenarioRunner runner = runner(excelReader);
                runner.runActiveScenarios();
            }
        });

        Assert.assertTrue(exception.getMessage().contains("Pre-run validation failed with 1 error(s)."));
        Assert.assertTrue(exception.getMessage().contains("Active scenario has no active testcase."));
        Assert.assertTrue(exception.getMessage().contains("Scenario NO"));
        Assert.assertTrue(exception.getMessage().contains("1"));
        Assert.assertTrue(exception.getMessage().contains("Local Keyword Test"));
    }

    @Test
    public void preRunValidationShouldFinishBeforeLazyKeywordEngineStartup() throws IOException {
        Path workbookPath = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("pre-run-before-driver.xlsx"),
                ValidationWorkbookFactory.scenarios(new Object[][]{{1, "Y", "Local Keyword Test", "Active scenario"}}),
                scenarioSheet("Local Keyword Test", new Object[][]{{"Inactive Login", "N", "", "", "", "", "Inactive only"}}),
                ValidationWorkbookFactory.objectRepository(new Object[][]{{"BRS", "btnLogin", "//button[@id='login']", "Login"}})
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            FakeWebDriver fakeWebDriver = new FakeWebDriver();
            DataReader dataReader = new DataReader(excelReader);
            ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
            AtomicInteger engineStartupCount = new AtomicInteger();
            ScenarioRunner runner = new ScenarioRunner(
                    new ScenarioReader(excelReader),
                    new StepReader(excelReader),
                    dataReader,
                    objectRepositoryReader,
                    () -> {
                        engineStartupCount.incrementAndGet();
                        return new KeywordEngine(
                                dataReader,
                                objectRepositoryReader,
                                new KeywordResolver(fakeWebDriver.driver())
                        );
                    }
            );

            FrameworkException exception = Assert.expectThrows(FrameworkException.class, runner::runActiveScenarios);

            Assert.assertTrue(exception.getMessage().contains("Pre-run validation failed with 1 error(s)."));
            Assert.assertTrue(exception.getMessage().contains("Active scenario has no active testcase."));
            Assert.assertEquals(engineStartupCount.get(), 0);
        }
    }

    private ScenarioRunner runner(ExcelReader excelReader) {
        FakeWebDriver fakeWebDriver = new FakeWebDriver();
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        KeywordResolver keywordResolver = new KeywordResolver(fakeWebDriver.driver());
        KeywordEngine keywordEngine = new KeywordEngine(dataReader, objectRepositoryReader, keywordResolver);
        return new ScenarioRunner(new ScenarioReader(excelReader), new StepReader(excelReader), keywordEngine);
    }
}
