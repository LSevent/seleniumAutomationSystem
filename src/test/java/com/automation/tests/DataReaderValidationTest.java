package com.automation.tests;

import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.tests.support.ValidationWorkbookFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;

import static com.automation.tests.support.ValidationWorkbookFactory.loginData;
import static com.automation.tests.support.ValidationWorkbookFactory.sheet;

public class DataReaderValidationTest {

    private static final Path TEMP_DIR = Path.of("target", "phase-11-data-validation");

    @Test
    public void bracketNotationShouldFailClearly() throws IOException {
        assertResolveFails(
                workbook("bracket.xlsx", loginData(new Object[][]{{1, "brs_admin", "brs123"}})),
                "LOGIN_DATA[USERNAME]",
                "Invalid data reference format: LOGIN_DATA[USERNAME]. Expected format: SHEET_NAME.COLUMN_NAME."
        );
    }

    @Test
    public void tooManyDotsShouldFailClearly() throws IOException {
        assertResolveFails(
                workbook("too-many-dots.xlsx", loginData(new Object[][]{{1, "brs_admin", "brs123"}})),
                "LOGIN_DATA.USER.NAME",
                "Invalid data reference format: LOGIN_DATA.USER.NAME. Expected format: SHEET_NAME.COLUMN_NAME."
        );
    }

    @Test
    public void missingSheetNameShouldFailClearly() throws IOException {
        assertResolveFails(
                workbook("missing-sheet-name.xlsx", loginData(new Object[][]{{1, "brs_admin", "brs123"}})),
                ".USERNAME",
                "Invalid data reference format: .USERNAME. Sheet name is required."
        );
    }

    @Test
    public void missingColumnNameShouldFailClearly() throws IOException {
        assertResolveFails(
                workbook("missing-column-name.xlsx", loginData(new Object[][]{{1, "brs_admin", "brs123"}})),
                "LOGIN_DATA.",
                "Invalid data reference format: LOGIN_DATA. Column name is required."
        );
    }

    @Test
    public void missingDataSheetShouldFailClearly() throws IOException {
        assertResolveFails(
                workbook("missing-data-sheet.xlsx", sheet("OTHER_DATA", new String[]{"NO", "USERNAME"}, new Object[][]{{1, "brs_admin"}})),
                "LOGIN_DATA.USERNAME",
                "Data sheet not found: LOGIN_DATA. Referenced by value LOGIN_DATA.USERNAME."
        );
    }

    @Test
    public void missingNoHeaderShouldFailClearly() throws IOException {
        assertResolveFails(
                workbook("missing-no-header.xlsx", sheet("LOGIN_DATA", new String[]{"USERNAME"}, new Object[][]{{"brs_admin"}})),
                "LOGIN_DATA.USERNAME",
                "Header not found: NO in sheet LOGIN_DATA."
        );
    }

    @Test
    public void missingDataColumnShouldFailClearly() throws IOException {
        assertResolveFails(
                workbook("missing-data-column.xlsx", sheet("LOGIN_DATA", new String[]{"NO", "PASSWORD"}, new Object[][]{{1, "brs123"}})),
                "LOGIN_DATA.USERNAME",
                "Header not found: USERNAME in sheet LOGIN_DATA."
        );
    }

    @Test
    public void missingScenarioNoShouldFailClearly() throws IOException {
        Path workbookPath = workbook("missing-scenario-no.xlsx", loginData(new Object[][]{{1, "brs_admin", "brs123"}}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new DataReader(excelReader).resolveValue("LOGIN_DATA.USERNAME", "");
            }
        });

        Assert.assertEquals(exception.getMessage(), "Scenario NO is required to resolve data reference LOGIN_DATA.USERNAME.");
    }

    @Test
    public void missingScenarioNoRowShouldFailClearly() throws IOException {
        assertResolveFails(
                workbook("missing-row.xlsx", loginData(new Object[][]{{1, "brs_admin", "brs123"}})),
                "LOGIN_DATA.USERNAME",
                "Data row not found in sheet LOGIN_DATA for NO = 2.",
                "2"
        );
    }

    private void assertResolveFails(Path workbookPath, String rawValue, String expectedMessage) {
        assertResolveFails(workbookPath, rawValue, expectedMessage, "1");
    }

    private void assertResolveFails(Path workbookPath, String rawValue, String expectedMessage, String scenarioNo) {
        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new DataReader(excelReader).resolveValue(rawValue, scenarioNo);
            }
        });

        Assert.assertEquals(exception.getMessage(), expectedMessage);
    }

    private Path workbook(String fileName, ValidationWorkbookFactory.SheetData... sheets) throws IOException {
        return ValidationWorkbookFactory.createWorkbook(TEMP_DIR.resolve(fileName), sheets);
    }
}
