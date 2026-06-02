package com.automation.tests;

import com.automation.excel.ExcelReader;
import com.automation.excel.StepReader;
import com.automation.models.Scenario;
import com.automation.tests.support.ValidationWorkbookFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;

import static com.automation.tests.support.ValidationWorkbookFactory.scenarioSheet;
import static com.automation.tests.support.ValidationWorkbookFactory.sheet;

public class StepReaderValidationTest {

    private static final Path TEMP_DIR = Path.of("target", "phase-11-step-validation");
    private static final String SHEET = "Local Keyword Test";

    @Test
    public void missingScenarioSheetShouldFailClearly() throws IOException {
        Path workbookPath = workbook("missing-scenario-sheet.xlsx",
                sheet("OTHER", new String[]{"A"}, new Object[][]{}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(SHEET));
            }
        });

        Assert.assertEquals(exception.getMessage(), "Scenario sheet not found: Local Keyword Test. Referenced by SCENARIOS row 2.");
    }

    @Test
    public void missingFunctionHeaderShouldFailClearly() throws IOException {
        Path workbookPath = workbook("missing-function-header.xlsx",
                sheet(SHEET, new String[]{"Testcase", "Run", "Object", "Value", "Application"}, new Object[][]{}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(SHEET));
            }
        });

        Assert.assertEquals(exception.getMessage(), "Header not found: Function in sheet Local Keyword Test.");
    }

    @Test
    public void stepBeforeTestcaseShouldFailClearly() throws IOException {
        Path workbookPath = workbook("step-before-testcase.xlsx",
                scenarioSheet(SHEET, new Object[][]{{"", "", "click", "btnLogin", "", "BRS", "Click login"}}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(SHEET));
            }
        });

        Assert.assertEquals(exception.getMessage(), "Step row found before any testcase parent row. Sheet: Local Keyword Test. Row: 2.");
    }

    @Test
    public void activeTestcaseMissingApplicationShouldFailClearly() throws IOException {
        Path workbookPath = workbook("missing-application.xlsx",
                scenarioSheet(SHEET, new Object[][]{
                        {"Login BRS", "Y", "", "", "", "", "Login"},
                        {"", "", "click", "btnLogin", "", "", "Click login"}
                }));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(SHEET));
            }
        });

        Assert.assertEquals(exception.getMessage(), "Application is required for active testcase 'Login BRS'. Sheet: Local Keyword Test. Row: 2.");
    }

    @Test
    public void invalidTestcaseRunShouldFailClearly() throws IOException {
        Path workbookPath = workbook("invalid-testcase-run.xlsx",
                scenarioSheet(SHEET, new Object[][]{
                        {"Login BRS", "MAYBE", "", "", "", "BRS", "Login"},
                        {"", "", "click", "btnLogin", "", "", "Click login"}
                }));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(SHEET));
            }
        });

        Assert.assertTrue(exception.getMessage().contains("Invalid Run value 'MAYBE' in sheet Local Keyword Test row 2."));
    }

    @Test
    public void testcaseParentWithStepFieldsShouldFailClearly() throws IOException {
        Path workbookPath = workbook("parent-with-step-fields.xlsx",
                scenarioSheet(SHEET, new Object[][]{
                        {"Login BRS", "Y", "click", "btnLogin", "", "BRS", "Login"},
                        {"", "", "click", "btnLogin", "", "", "Click login"}
                }));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(SHEET));
            }
        });

        Assert.assertEquals(exception.getMessage(), "Testcase parent row should not contain Function, Object, or Value. Sheet: Local Keyword Test. Row: 2.");
    }

    @Test
    public void stepMissingFunctionShouldFailClearly() throws IOException {
        Path workbookPath = workbook("step-missing-function.xlsx",
                scenarioSheet(SHEET, new Object[][]{
                        {"Login BRS", "Y", "", "", "", "BRS", "Login"},
                        {"", "", "", "btnLogin", "", "", "Click login"}
                }));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(SHEET));
            }
        });

        Assert.assertEquals(exception.getMessage(), "Function is required for step row. Sheet: Local Keyword Test. Row: 3.");
    }

    @Test
    public void activeTestcaseWithNoStepsShouldFailClearly() throws IOException {
        Path workbookPath = workbook("no-steps.xlsx",
                scenarioSheet(SHEET, new Object[][]{{"Login BRS", "Y", "", "", "", "BRS", "Login"}}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(SHEET));
            }
        });

        Assert.assertEquals(exception.getMessage(), "Active testcase 'Login BRS' has no steps. Sheet: Local Keyword Test. Row: 2.");
    }

    @Test
    public void duplicateTestcaseNameShouldFailClearly() throws IOException {
        Path workbookPath = workbook("duplicate-testcase.xlsx",
                scenarioSheet(SHEET, new Object[][]{
                        {"Login BRS", "Y", "", "", "", "BRS", "Login"},
                        {"", "", "click", "btnLogin", "", "", "Click login"},
                        {"Login BRS", "N", "", "", "", "", "Duplicate"}
                }));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(SHEET));
            }
        });

        Assert.assertEquals(exception.getMessage(), "Duplicate testcase name 'Login BRS' found in sheet Local Keyword Test.");
    }

    private Scenario scenario(String action) {
        return new Scenario("1", true, action, "Validation scenario", 2);
    }

    private Path workbook(String fileName, ValidationWorkbookFactory.SheetData... sheets) throws IOException {
        return ValidationWorkbookFactory.createWorkbook(TEMP_DIR.resolve(fileName), sheets);
    }
}
