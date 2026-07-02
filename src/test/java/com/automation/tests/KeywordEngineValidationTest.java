package com.automation.tests;

import com.automation.engine.KeywordResolver;
import com.automation.engine.KeywordEngine;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.models.ExecutionResult;
import com.automation.models.ResolvedStepContext;
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
    public void keywordResolutionErrorShouldIncludeStepContext() {
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ExecutionResult result = engine(excelReader).execute(
                    step("approveBooking", "", "", "", "", 7)
            );

            Assert.assertFalse(result.isSuccess());
            Assert.assertTrue(result.getMessage().contains("Keyword 'approveBooking' failed at step row 7."));
            Assert.assertTrue(result.getMessage().contains("Application: BRS."));
            Assert.assertTrue(result.getMessage().contains("Scenario ACTION: Local Keyword Test."));
            Assert.assertTrue(result.getMessage().contains("Cause: Keyword 'approveBooking' not found in SpecificFunction for application 'BRS' or BaseFunction."));
        }
    }

    @Test
    public void blankKeywordShouldIncludeSheetAndRow() {
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ExecutionResult result = engine(excelReader).execute(
                    step("", "btnLogin", "", "//button[@id='loginButton']", "//button[@id='loginButton']", 8)
            );

            Assert.assertFalse(result.isSuccess());
            Assert.assertTrue(result.getMessage().contains("Keyword is required in sheet Local Keyword Test row 8."));
            Assert.assertTrue(result.getMessage().contains("Scenario NO: 1."));
            Assert.assertTrue(result.getMessage().contains("Testcase: Login BRS."));
            Assert.assertTrue(result.getMessage().contains("Object: btnLogin."));
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
                new KeywordResolver(fakeWebDriver.driver()),
                new ExcelReportConfig(false, true, true)
        );
    }

    private ResolvedStepContext step(
            String keyword,
            String object,
            String rawValue,
            String rawXPath,
            String resolvedXPath,
            int row
    ) {
        return ResolvedStepContext.builder()
                .scenarioNo("1")
                .scenarioAction("Local Keyword Test")
                .scenarioName("Local keyword execution test")
                .sheetName("Local Keyword Test")
                .testcaseName("Login BRS")
                .testcaseParentRow(3)
                .excelRow(row)
                .stepNumber(1)
                .keyword(keyword)
                .objectName(object)
                .application("BRS")
                .description("Validation step")
                .rawValue(rawValue)
                .resolvedValue(rawValue)
                .rawXPath(rawXPath)
                .resolvedXPath(resolvedXPath)
                .executedBy("")
                .build();
    }
}
