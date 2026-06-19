package com.automation.tests;

import com.automation.engine.FunctionResolver;
import com.automation.engine.KeywordEngine;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.models.ExecutionResult;
import com.automation.models.Scenario;
import com.automation.models.TestStep;
import com.automation.reports.ExcelReportConfig;
import com.automation.tests.support.ExcelKeywordTestWorkbookFactory;
import com.automation.tests.support.FakeWebDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;

public class KeywordEngineValidationTest {

    private static final Path TEMP_DIR = Path.of("target", "phase-11-keyword-engine-validation");
    private static final Path LOCAL_HTML = Path.of("src", "test", "resources", "test-pages", "excel-keyword-test.html");

    private Path workbookPath;

    @BeforeClass
    public void createWorkbook() throws IOException {
        workbookPath = ExcelKeywordTestWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("keyword-engine-validation.xlsx"),
                LOCAL_HTML.toAbsolutePath().toUri().toString()
        );
    }

    @Test
    public void dataResolutionErrorShouldIncludeStepContext() {
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ExecutionResult result = engine(excelReader).executeStep(
                    scenario(),
                    step("input", "txtUsername", "MISSING_DATA.USERNAME", 5)
            );

            Assert.assertFalse(result.isSuccess());
            Assert.assertTrue(result.getMessage().contains("Failed to resolve value for step row 5."));
            Assert.assertTrue(result.getMessage().contains("Raw value: MISSING_DATA.USERNAME."));
            Assert.assertTrue(result.getMessage().contains("Scenario NO: 1."));
            Assert.assertTrue(result.getMessage().contains("Scenario ACTION: Local Keyword Test."));
            Assert.assertTrue(result.getMessage().contains("Testcase: Login BRS."));
            Assert.assertTrue(result.getMessage().contains("Cause: Data sheet not found: MISSING_DATA."));
        }
    }

    @Test
    public void objectResolutionErrorShouldIncludeStepContext() {
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ExecutionResult result = engine(excelReader).executeStep(
                    scenario(),
                    step("click", "btnMissing", "", 6)
            );

            Assert.assertFalse(result.isSuccess());
            Assert.assertTrue(result.getMessage().contains("Failed to resolve object for step row 6."));
            Assert.assertTrue(result.getMessage().contains("Application: BRS."));
            Assert.assertTrue(result.getMessage().contains("Object: btnMissing."));
            Assert.assertTrue(result.getMessage().contains("Scenario NO: 1."));
            Assert.assertTrue(result.getMessage().contains("Testcase: Login BRS."));
        }
    }

    @Test
    public void functionResolutionErrorShouldIncludeStepContext() {
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ExecutionResult result = engine(excelReader).executeStep(
                    scenario(),
                    step("approveBooking", "", "", 7)
            );

            Assert.assertFalse(result.isSuccess());
            Assert.assertTrue(result.getMessage().contains("Keyword 'approveBooking' failed at step row 7."));
            Assert.assertTrue(result.getMessage().contains("Application: BRS."));
            Assert.assertTrue(result.getMessage().contains("Scenario ACTION: Local Keyword Test."));
            Assert.assertTrue(result.getMessage().contains("Cause: Keyword 'approveBooking' not found in SpecificFunction for application 'BRS' or BaseFunction."));
        }
    }

    @Test
    public void blankFunctionShouldIncludeSheetAndRow() {
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ExecutionResult result = engine(excelReader).executeStep(
                    scenario(),
                    step("", "btnLogin", "", 8)
            );

            Assert.assertFalse(result.isSuccess());
            Assert.assertEquals(result.getMessage(), "Function is required in sheet Local Keyword Test row 8.");
        }
    }

    private KeywordEngine engine(ExcelReader excelReader) {
        FakeWebDriver fakeWebDriver = new FakeWebDriver();
        fakeWebDriver.addElement("//input[@id='username']", "");
        fakeWebDriver.addElement("//button[@id='loginButton']", "Login");
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        return new KeywordEngine(
                dataReader,
                objectRepositoryReader,
                new FunctionResolver(fakeWebDriver.driver()),
                new ExcelReportConfig(false, true, true)
        );
    }

    private Scenario scenario() {
        return new Scenario("1", true, "Local Keyword Test", "Local keyword execution test", 2);
    }

    private TestStep step(String function, String object, String value, int row) {
        return new TestStep("1", "Local keyword execution test", "Local Keyword Test", "Login BRS",
                function, object, value, "BRS", "Validation step", row, 1);
    }
}
