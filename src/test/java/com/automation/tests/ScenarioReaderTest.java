package com.automation.tests;

import com.automation.excel.ExcelReader;
import com.automation.excel.ExcelTemplateFormatter;
import com.automation.excel.ScenarioReader;
import com.automation.models.Scenario;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ScenarioReaderTest {

    private static final Path TEMPLATE_FILE = Path.of("src", "test", "resources", "testdata", "Template Testing.xlsx");
    private static final Path TEMP_DIR = Path.of("target", "scenario-reader-test");
    private static final int MIN_TEMPLATE_COLUMN_WIDTH = 10 * 256;
    private static final int MAX_TEMPLATE_COLUMN_WIDTH = 45 * 256;
    private static final String[] SCENARIO_SHEET_HEADERS = {
            "Testcase", "Run", "Function", "Object", "Value", "Application", "Description"
    };

    @BeforeClass
    public void createTempDirectory() throws IOException {
        Files.createDirectories(TEMP_DIR);
    }

    @Test
    public void shouldReadAllScenarios() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ScenarioReader scenarioReader = new ScenarioReader(excelReader);

            List<Scenario> scenarios = scenarioReader.getAllScenarios();

            Assert.assertEquals(scenarios.size(), 3);
        }
    }

    @Test
    public void shouldReadActiveScenariosOnly() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ScenarioReader scenarioReader = new ScenarioReader(excelReader);

            List<Scenario> activeScenarios = scenarioReader.getActiveScenarios();

            Assert.assertEquals(activeScenarios.size(), 2);
            Assert.assertTrue(activeScenarios.stream().allMatch(scenario -> "Create New Booking".equals(scenario.getAction())));
        }
    }

    @Test
    public void runNScenarioShouldBeIgnoredByActiveScenarios() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ScenarioReader scenarioReader = new ScenarioReader(excelReader);

            List<Scenario> activeScenarios = scenarioReader.getActiveScenarios();

            Assert.assertTrue(activeScenarios.stream().noneMatch(scenario -> "Cancel Booking".equals(scenario.getAction())));
        }
    }

    @Test
    public void actionSheetValidationShouldPassForTemplateWorkbook() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ScenarioReader scenarioReader = new ScenarioReader(excelReader);

            Assert.assertTrue(excelReader.isSheetExists("Create New Booking"));
            Assert.assertTrue(excelReader.isSheetExists("Cancel Booking"));
            scenarioReader.validateScenarios();
        }
    }

    @Test
    public void scenarioSheetsShouldUseAgreedTemplateHeaders() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            assertScenarioSheetHeaders(excelReader, "Create New Booking");
            assertScenarioSheetHeaders(excelReader, "Cancel Booking");
        }
    }

    @Test
    public void templateWorkbookShouldFreezeHeadersAndFitUsedColumns() throws IOException {
        try (InputStream inputStream = Files.newInputStream(TEMPLATE_FILE);
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            for (Sheet sheet : workbook) {
                Assert.assertNotNull(sheet.getPaneInformation(), sheet.getSheetName() + " should freeze the header row.");
                Assert.assertTrue(sheet.getPaneInformation().isFreezePane(), sheet.getSheetName() + " should use a frozen pane.");
                Assert.assertEquals(sheet.getPaneInformation().getHorizontalSplitPosition(), 1, sheet.getSheetName() + " should freeze row 1.");

                Row headerRow = sheet.getRow(0);
                Assert.assertNotNull(headerRow, sheet.getSheetName() + " should have a header row.");
                for (int columnIndex = 0; columnIndex < headerRow.getLastCellNum(); columnIndex++) {
                    int columnWidth = sheet.getColumnWidth(columnIndex);
                    Assert.assertTrue(columnWidth >= MIN_TEMPLATE_COLUMN_WIDTH, sheet.getSheetName() + " column " + columnIndex + " should meet the minimum width.");
                    Assert.assertTrue(columnWidth <= MAX_TEMPLATE_COLUMN_WIDTH, sheet.getSheetName() + " column " + columnIndex + " should stay within the maximum width.");
                }
            }
        }
    }

    @Test
    public void shouldUseNoAsDataKeyForActiveScenarios() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ScenarioReader scenarioReader = new ScenarioReader(excelReader);

            List<Scenario> activeScenarios = scenarioReader.getActiveScenarios();

            Assert.assertEquals(activeScenarios.get(0).getNo(), "1");
            Assert.assertEquals(activeScenarios.get(0).getDataKey(), "1");
            Assert.assertEquals(activeScenarios.get(1).getNo(), "2");
            Assert.assertEquals(activeScenarios.get(1).getDataKey(), "2");
        }
    }

    @Test
    public void invalidRunValueShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "invalid-run.xlsx",
                new String[]{"NO", "RUN", "ACTION", "SCENARIOS"},
                new Object[][]{
                        {1, "MAYBE", "Create New Booking", "Create booking room A"}
                },
                "Create New Booking"
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new ScenarioReader(excelReader).getActiveScenarios();
            }
        });

        Assert.assertTrue(exception.getMessage().contains("Invalid RUN value 'MAYBE' in SCENARIOS row 2."));
    }

    @Test
    public void missingActionSheetShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "missing-action-sheet.xlsx",
                new String[]{"NO", "RUN", "ACTION", "SCENARIOS"},
                new Object[][]{
                        {1, "Y", "Create New Booking", "Create booking room A"}
                }
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new ScenarioReader(excelReader).getActiveScenarios();
            }
        });

        Assert.assertTrue(exception.getMessage().contains("Scenario sheet not found: Create New Booking. Referenced by SCENARIOS row 2."));
    }

    @Test
    public void missingRequiredHeaderShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "missing-action-header.xlsx",
                new String[]{"NO", "RUN", "SCENARIOS"},
                new Object[][]{
                        {1, "Y", "Create booking room A"}
                },
                "Create New Booking"
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new ScenarioReader(excelReader).getAllScenarios();
            }
        });

        Assert.assertTrue(exception.getMessage().contains("Header not found: ACTION in sheet SCENARIOS"));
    }

    @Test
    public void blankNoForActiveScenarioShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "blank-no.xlsx",
                new String[]{"NO", "RUN", "ACTION", "SCENARIOS"},
                new Object[][]{
                        {"", "Y", "Create New Booking", "Create booking room A"}
                },
                "Create New Booking"
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new ScenarioReader(excelReader).getActiveScenarios();
            }
        });

        Assert.assertTrue(exception.getMessage().contains("NO is required for active scenario at SCENARIOS row 2."));
    }

    @Test
    public void duplicateNoShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "duplicate-no.xlsx",
                new String[]{"NO", "RUN", "ACTION", "SCENARIOS"},
                new Object[][]{
                        {1, "Y", "Create New Booking", "Create booking room A"},
                        {1, "N", "Cancel Booking", "Cancel booking"}
                },
                "Create New Booking",
                "Cancel Booking"
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new ScenarioReader(excelReader).getAllScenarios();
            }
        });

        Assert.assertTrue(exception.getMessage().contains("Duplicate NO value '1' in SCENARIOS row 3. First used in SCENARIOS row 2."));
    }

    private Path createWorkbook(String fileName, String[] headers, Object[][] scenarioRows, String... scenarioSheetNames) throws IOException {
        Path workbookPath = TEMP_DIR.resolve(fileName);
        try (Workbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(workbookPath)) {
            Sheet scenariosSheet = workbook.createSheet("SCENARIOS");
            writeRow(scenariosSheet.createRow(0), headers);

            for (int rowIndex = 0; rowIndex < scenarioRows.length; rowIndex++) {
                writeRow(scenariosSheet.createRow(rowIndex + 1), scenarioRows[rowIndex]);
            }

            for (String scenarioSheetName : scenarioSheetNames) {
                Sheet scenarioSheet = workbook.createSheet(scenarioSheetName);
                writeRow(scenarioSheet.createRow(0), SCENARIO_SHEET_HEADERS);
            }

            ExcelTemplateFormatter.applyReadableLayout(workbook);
            workbook.write(outputStream);
        }
        return workbookPath;
    }

    private void assertScenarioSheetHeaders(ExcelReader excelReader, String sheetName) {
        Assert.assertEquals(excelReader.getColumnCount(sheetName, 0), SCENARIO_SHEET_HEADERS.length);
        for (int columnIndex = 0; columnIndex < SCENARIO_SHEET_HEADERS.length; columnIndex++) {
            Assert.assertEquals(excelReader.getCellValue(sheetName, 0, columnIndex), SCENARIO_SHEET_HEADERS[columnIndex]);
        }
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
}
