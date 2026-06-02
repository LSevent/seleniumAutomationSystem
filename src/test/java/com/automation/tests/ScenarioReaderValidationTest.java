package com.automation.tests;

import com.automation.excel.ExcelReader;
import com.automation.excel.ScenarioReader;
import com.automation.tests.support.ValidationWorkbookFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;

import static com.automation.tests.support.ValidationWorkbookFactory.scenarioSheet;
import static com.automation.tests.support.ValidationWorkbookFactory.scenarios;
import static com.automation.tests.support.ValidationWorkbookFactory.sheet;

public class ScenarioReaderValidationTest {

    private static final Path TEMP_DIR = Path.of("target", "phase-11-scenario-validation");

    @Test
    public void missingScenariosSheetShouldFailClearly() throws IOException {
        Path workbookPath = workbook("missing-scenarios.xlsx",
                scenarioSheet("Local Keyword Test", new Object[][]{}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new ScenarioReader(excelReader).getAllScenarios();
            }
        });

        Assert.assertEquals(exception.getMessage(), "Required sheet not found: SCENARIOS.");
    }

    @Test
    public void missingNoHeaderShouldFailClearly() throws IOException {
        Path workbookPath = workbook("missing-no-header.xlsx",
                sheet("SCENARIOS", new String[]{"RUN", "ACTION", "SCENARIOS"}, new Object[][]{{"Y", "Local Keyword Test", "Run login"}}),
                scenarioSheet("Local Keyword Test", new Object[][]{}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new ScenarioReader(excelReader).getAllScenarios();
            }
        });

        Assert.assertEquals(exception.getMessage(), "Header not found: NO in sheet SCENARIOS.");
    }

    @Test
    public void invalidRunValueShouldFailClearly() throws IOException {
        Path workbookPath = workbook("invalid-run.xlsx",
                scenarios(new Object[][]{{1, "MAYBE", "Local Keyword Test", "Run login"}}),
                scenarioSheet("Local Keyword Test", new Object[][]{}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new ScenarioReader(excelReader).getAllScenarios();
            }
        });

        Assert.assertTrue(exception.getMessage().contains("Invalid RUN value 'MAYBE' in sheet SCENARIOS row 2."));
    }

    @Test
    public void blankNoOnNonEmptyScenarioRowShouldFailClearly() throws IOException {
        Path workbookPath = workbook("blank-no.xlsx",
                scenarios(new Object[][]{{"", "N", "Local Keyword Test", "Inactive but malformed"}}),
                scenarioSheet("Local Keyword Test", new Object[][]{}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new ScenarioReader(excelReader).getAllScenarios();
            }
        });

        Assert.assertEquals(exception.getMessage(), "Scenario NO is required in sheet SCENARIOS row 2.");
    }

    @Test
    public void activeScenarioMissingActionShouldFailClearly() throws IOException {
        Path workbookPath = workbook("missing-action.xlsx",
                scenarios(new Object[][]{{1, "Y", "", "Run login"}}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new ScenarioReader(excelReader).getActiveScenarios();
            }
        });

        Assert.assertEquals(exception.getMessage(), "ACTION is required for active scenario in sheet SCENARIOS row 2.");
    }

    @Test
    public void duplicateNoShouldFailClearly() throws IOException {
        Path workbookPath = workbook("duplicate-no.xlsx",
                scenarios(new Object[][]{
                        {1, "Y", "Local Keyword Test", "Run login"},
                        {1, "N", "Other Flow", "Inactive duplicate"}
                }),
                scenarioSheet("Local Keyword Test", new Object[][]{}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new ScenarioReader(excelReader).getAllScenarios();
            }
        });

        Assert.assertEquals(exception.getMessage(), "Duplicate Scenario NO found in sheet SCENARIOS: 1.");
    }

    @Test
    public void duplicateActiveScenarioIdentityShouldFailClearly() throws IOException {
        Path workbookPath = workbook("duplicate-active-identity.xlsx",
                scenarios(new Object[][]{
                        {1, "Y", "Local Keyword Test", "Run login"},
                        {1, "Y", "Local Keyword Test", "Run login duplicate"}
                }),
                scenarioSheet("Local Keyword Test", new Object[][]{}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new ScenarioReader(excelReader).getAllScenarios();
            }
        });

        Assert.assertEquals(exception.getMessage(), "Duplicate active scenario row found for NO = 1 and ACTION = Local Keyword Test.");
    }

    private Path workbook(String fileName, ValidationWorkbookFactory.SheetData... sheets) throws IOException {
        return ValidationWorkbookFactory.createWorkbook(TEMP_DIR.resolve(fileName), sheets);
    }
}
