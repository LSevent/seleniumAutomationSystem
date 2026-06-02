package com.automation.tests;

import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.models.DataReference;
import com.automation.models.Scenario;
import com.automation.models.TestStep;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DataReaderTest {

    private static final Path TEMPLATE_FILE = Path.of("src", "test", "resources", "testdata", "Template Testing.xlsx");
    private static final Path TEMP_DIR = Path.of("target", "data-reader-test");

    @BeforeClass
    public void createTempDirectory() throws IOException {
        Files.createDirectories(TEMP_DIR);
    }

    @Test
    public void shouldDetectValidDataReference() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            DataReader dataReader = new DataReader(excelReader);

            Assert.assertTrue(dataReader.isDataReference("LOGIN_DATA.USERNAME"));
        }
    }

    @Test
    public void literalValueShouldNotBeTreatedAsDataReference() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            DataReader dataReader = new DataReader(excelReader);

            Assert.assertFalse(dataReader.isDataReference("Booking created successfully"));
            Assert.assertEquals(dataReader.resolveValue("Booking created successfully", "1"), "Booking created successfully");
        }
    }

    @Test
    public void shouldParseDataReference() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            DataReader dataReader = new DataReader(excelReader);

            DataReference dataReference = dataReader.parseReference("BOOKING_DATA.ROOM_NAME");

            Assert.assertEquals(dataReference.getRawReference(), "BOOKING_DATA.ROOM_NAME");
            Assert.assertEquals(dataReference.getSheetName(), "BOOKING_DATA");
            Assert.assertEquals(dataReference.getColumnName(), "ROOM_NAME");
        }
    }

    @Test
    public void shouldTrimReferenceParts() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            DataReader dataReader = new DataReader(excelReader);

            DataReference dataReference = dataReader.parseReference("LOGIN_DATA . USERNAME");

            Assert.assertEquals(dataReference.getSheetName(), "LOGIN_DATA");
            Assert.assertEquals(dataReference.getColumnName(), "USERNAME");
        }
    }

    @Test
    public void shouldResolveLoginUsernameForScenarioOne() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            DataReader dataReader = new DataReader(excelReader);

            Assert.assertEquals(dataReader.resolveValue("LOGIN_DATA.USERNAME", "1"), "brs_admin");
        }
    }

    @Test
    public void shouldResolveLoginUsernameForScenarioTwo() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            DataReader dataReader = new DataReader(excelReader);

            Assert.assertEquals(dataReader.resolveValue("LOGIN_DATA.USERNAME", "2"), "brs_user2");
        }
    }

    @Test
    public void shouldResolveBookingRoomNameForScenarioOne() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            DataReader dataReader = new DataReader(excelReader);

            Assert.assertEquals(dataReader.resolveValue("BOOKING_DATA.ROOM_NAME", "1"), "Meeting Room A");
        }
    }

    @Test
    public void shouldResolveBookingRoomNameForScenarioTwo() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            DataReader dataReader = new DataReader(excelReader);

            Assert.assertEquals(dataReader.resolveValue("BOOKING_DATA.ROOM_NAME", "2"), "Meeting Room B");
        }
    }

    @Test
    public void blankAndNullValuesShouldResolveToEmptyString() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            DataReader dataReader = new DataReader(excelReader);

            Assert.assertEquals(dataReader.resolveValue("", "1"), "");
            Assert.assertEquals(dataReader.resolveValue("   ", "1"), "");
            Assert.assertEquals(dataReader.resolveValue(null, "1"), "");
        }
    }

    @Test
    public void missingDataSheetShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "missing-data-sheet.xlsx",
                new SheetData("OTHER_DATA", new String[]{"NO", "USERNAME"}, new Object[][]{{1, "brs_admin"}})
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new DataReader(excelReader).resolveValue("LOGIN_DATA.USERNAME", "1");
            }
        });

        Assert.assertEquals(exception.getMessage(), "Data sheet not found: LOGIN_DATA. Referenced by value LOGIN_DATA.USERNAME.");
    }

    @Test
    public void missingNoColumnShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "missing-no-column.xlsx",
                new SheetData("LOGIN_DATA", new String[]{"USERNAME"}, new Object[][]{{"brs_admin"}})
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new DataReader(excelReader).resolveValue("LOGIN_DATA.USERNAME", "1");
            }
        });

        Assert.assertEquals(exception.getMessage(), "Header not found: NO in sheet LOGIN_DATA.");
    }

    @Test
    public void missingScenarioNoRowShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "missing-scenario-no.xlsx",
                loginDataSheet(new Object[][]{{1, "brs_admin", "brs123"}})
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new DataReader(excelReader).resolveValue("LOGIN_DATA.USERNAME", "2");
            }
        });

        Assert.assertEquals(exception.getMessage(), "Data row not found in sheet LOGIN_DATA for NO = 2.");
    }

    @Test
    public void missingDataColumnShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "missing-data-column.xlsx",
                new SheetData("LOGIN_DATA", new String[]{"NO", "PASSWORD"}, new Object[][]{{1, "brs123"}})
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new DataReader(excelReader).resolveValue("LOGIN_DATA.USERNAME", "1");
            }
        });

        Assert.assertEquals(exception.getMessage(), "Header not found: USERNAME in sheet LOGIN_DATA.");
    }

    @Test
    public void bracketNotationShouldThrowClearError() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            DataReader dataReader = new DataReader(excelReader);

            IllegalArgumentException exception = Assert.expectThrows(
                    IllegalArgumentException.class,
                    () -> dataReader.resolveValue("LOGIN_DATA[USERNAME]", "1")
            );

            Assert.assertEquals(exception.getMessage(), "Invalid data reference format: LOGIN_DATA[USERNAME]. Expected format: SHEET_NAME.COLUMN_NAME.");
        }
    }

    @Test
    public void tooManyDotsShouldThrowClearError() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            DataReader dataReader = new DataReader(excelReader);

            IllegalArgumentException exception = Assert.expectThrows(
                    IllegalArgumentException.class,
                    () -> dataReader.resolveValue("LOGIN_DATA.USER.NAME", "1")
            );

            Assert.assertEquals(exception.getMessage(), "Invalid data reference format: LOGIN_DATA.USER.NAME. Expected format: SHEET_NAME.COLUMN_NAME.");
        }
    }

    @Test
    public void missingSheetNameShouldThrowClearError() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            DataReader dataReader = new DataReader(excelReader);

            IllegalArgumentException exception = Assert.expectThrows(
                    IllegalArgumentException.class,
                    () -> dataReader.resolveValue(".USERNAME", "1")
            );

            Assert.assertEquals(exception.getMessage(), "Invalid data reference format: .USERNAME. Sheet name is required.");
        }
    }

    @Test
    public void missingColumnNameShouldThrowClearError() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            DataReader dataReader = new DataReader(excelReader);

            IllegalArgumentException exception = Assert.expectThrows(
                    IllegalArgumentException.class,
                    () -> dataReader.resolveValue("LOGIN_DATA.", "1")
            );

            Assert.assertEquals(exception.getMessage(), "Invalid data reference format: LOGIN_DATA. Column name is required.");
        }
    }

    @Test
    public void blankScenarioNoShouldThrowClearError() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            DataReader dataReader = new DataReader(excelReader);

            IllegalArgumentException exception = Assert.expectThrows(
                    IllegalArgumentException.class,
                    () -> dataReader.resolveValue("LOGIN_DATA.USERNAME", "")
            );

            Assert.assertEquals(exception.getMessage(), "Scenario NO is required to resolve data reference LOGIN_DATA.USERNAME.");
        }
    }

    @Test
    public void shouldResolveValuesFromParsedTestSteps() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            Scenario scenario = new ScenarioReader(excelReader).getActiveScenarios().get(0);
            List<TestStep> activeSteps = new StepReader(excelReader).getActiveSteps(scenario);
            DataReader dataReader = new DataReader(excelReader);

            String usernameValue = activeSteps.stream()
                    .map(TestStep::getValue)
                    .filter("LOGIN_DATA.USERNAME"::equals)
                    .findFirst()
                    .orElseThrow();
            String roomNameValue = activeSteps.stream()
                    .map(TestStep::getValue)
                    .filter("BOOKING_DATA.ROOM_NAME"::equals)
                    .findFirst()
                    .orElseThrow();

            Assert.assertEquals(dataReader.resolveValue(usernameValue, scenario), "brs_admin");
            Assert.assertEquals(dataReader.resolveValue(roomNameValue, scenario), "Meeting Room A");
        }
    }

    private Path createWorkbook(String fileName, SheetData... sheets) throws IOException {
        Path workbookPath = TEMP_DIR.resolve(fileName);
        try (Workbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(workbookPath)) {
            for (SheetData sheetData : sheets) {
                Sheet sheet = workbook.createSheet(sheetData.name());
                writeRow(sheet.createRow(0), sheetData.headers());
                for (int rowIndex = 0; rowIndex < sheetData.rows().length; rowIndex++) {
                    writeRow(sheet.createRow(rowIndex + 1), sheetData.rows()[rowIndex]);
                }
            }
            workbook.write(outputStream);
        }
        return workbookPath;
    }

    private SheetData loginDataSheet(Object[][] rows) {
        return new SheetData("LOGIN_DATA", new String[]{"NO", "USERNAME", "PASSWORD"}, rows);
    }

    private void writeRow(Row row, Object[] values) {
        for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
            Object value = values[columnIndex];
            if (value instanceof Number number) {
                row.createCell(columnIndex).setCellValue(number.doubleValue());
            } else if (value instanceof Boolean bool) {
                row.createCell(columnIndex).setCellValue(bool);
            } else if (value != null) {
                row.createCell(columnIndex).setCellValue(value.toString());
            }
        }
    }

    private record SheetData(String name, String[] headers, Object[][] rows) {
    }
}
