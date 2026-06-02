package com.automation.tests;

import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.models.ResolvedObject;
import com.automation.models.Scenario;
import com.automation.models.TestCaseBlock;
import com.automation.models.TestObject;
import com.automation.models.TestStep;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FinalTemplateValidationTest {

    private static final Path FINAL_TEMPLATE = Path.of(
            "src",
            "test",
            "resources",
            "testdata",
            "Final Excel Template.xlsx"
    );

    private static final List<String> REQUIRED_SHEETS = List.of(
            "SCENARIOS",
            "CONFIG",
            "LOGIN_DATA",
            "BOOKING_DATA",
            "Local Keyword Test",
            "Create New Booking",
            "Cancel Booking",
            "OBJECT_REPOSITORY"
    );

    private static final List<String> SCENARIO_SHEETS = List.of(
            "Local Keyword Test",
            "Create New Booking",
            "Cancel Booking"
    );

    private static final List<String> SCENARIOS_HEADERS = List.of("NO", "RUN", "ACTION", "SCENARIOS");
    private static final List<String> SCENARIO_SHEET_HEADERS = List.of(
            "Testcase",
            "Run",
            "Function",
            "Object",
            "Value",
            "Application",
            "Description"
    );
    private static final List<String> CURRENT_OBJECT_REPOSITORY_HEADERS = List.of(
            "Object",
            "XPath",
            "Application",
            "Description"
    );
    private static final Set<String> REQUIRED_DATA_REFERENCES = Set.of(
            "CONFIG.BASE_URL",
            "LOGIN_DATA.USERNAME",
            "LOGIN_DATA.PASSWORD",
            "BOOKING_DATA.BOOKING_TITLE",
            "BOOKING_DATA.ROOM_NAME",
            "BOOKING_DATA.EXPECTED_MESSAGE",
            "BOOKING_DATA.BOOKING_ID"
    );
    private static final Map<String, String> EXPECTED_OBJECTS = Map.ofEntries(
            Map.entry("BRS::txtUsername", "//input[@id='username']"),
            Map.entry("BRS::txtPassword", "//input[@id='password']"),
            Map.entry("BRS::btnLogin", "//button[@id='loginButton']"),
            Map.entry("BRS::lblDashboard", "//h1[@id='dashboard']"),
            Map.entry("BRS::txtBookingTitle", "//input[@id='bookingTitle']"),
            Map.entry("BRS::btnRoomByName", "//button[contains(text(),'{ROOM_NAME}')]"),
            Map.entry("BRS::lblSuccessMessage", "//div[@id='message']"),
            Map.entry("BRS::btnCancelBooking", "//button[@data-booking='{BOOKING_ID}']"),
            Map.entry("HRIS::txtUsername", "//input[@id='employeeId']"),
            Map.entry("HRIS::txtPassword", "//input[@id='password']"),
            Map.entry("HRIS::btnLogin", "//button[@id='loginButton']")
    );

    @Test
    public void finalTemplateShouldExist() {
        Assert.assertTrue(Files.exists(FINAL_TEMPLATE), "Final Excel template should exist.");
        Assert.assertTrue(Files.isRegularFile(FINAL_TEMPLATE), "Final Excel template should be a file.");
    }

    @Test
    public void finalTemplateShouldHaveRequiredSheetsAndHeaders() throws IOException {
        try (InputStream inputStream = Files.newInputStream(FINAL_TEMPLATE);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            for (String sheetName : REQUIRED_SHEETS) {
                Assert.assertNotNull(workbook.getSheet(sheetName), "Required sheet should exist: " + sheetName);
            }

            assertHeadersInOrder(workbook.getSheet("SCENARIOS"), SCENARIOS_HEADERS);
            assertHeadersInOrder(workbook.getSheet("CONFIG"), List.of("NO", "BASE_URL"));
            assertHeadersInOrder(workbook.getSheet("LOGIN_DATA"), List.of("NO", "USERNAME", "PASSWORD"));
            assertHeadersInOrder(workbook.getSheet("BOOKING_DATA"), List.of("NO", "BOOKING_TITLE", "ROOM_NAME", "EXPECTED_MESSAGE", "BOOKING_ID"));

            for (String scenarioSheet : SCENARIO_SHEETS) {
                assertHeadersInOrder(workbook.getSheet(scenarioSheet), SCENARIO_SHEET_HEADERS);
            }

            assertHeadersInOrder(workbook.getSheet("OBJECT_REPOSITORY"), CURRENT_OBJECT_REPOSITORY_HEADERS);
        }
    }

    @Test
    public void finalTemplateShouldBeReadableByScenarioAndStepReaders() {
        try (ExcelReader excelReader = new ExcelReader(FINAL_TEMPLATE.toString())) {
            ScenarioReader scenarioReader = new ScenarioReader(excelReader);
            StepReader stepReader = new StepReader(excelReader);

            List<Scenario> scenarios = scenarioReader.getAllScenarios();
            Assert.assertEquals(scenarios.size(), 3);
            Assert.assertEquals(scenarioReader.getActiveScenarios().size(), 1);

            for (Scenario scenario : scenarios) {
                Assert.assertTrue(excelReader.isSheetExists(scenario.getAction()), "Scenario ACTION should point to an existing sheet.");
                List<TestCaseBlock> testCases = stepReader.getTestCases(scenario);
                Assert.assertFalse(testCases.isEmpty(), "Scenario sheet should contain testcase blocks: " + scenario.getAction());
                assertStepOrderAndInheritedApplication(testCases);
            }
        }
    }

    @Test
    public void finalTemplateShouldUseDotDataReferencesAndNoRemovedColumns() throws IOException {
        try (InputStream inputStream = Files.newInputStream(FINAL_TEMPLATE);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            assertWorkbookDoesNotContainRemovedColumn(workbook);
            assertScenarioValuesUseDotReferences(workbook);
        }
    }

    @Test
    public void finalTemplateDataReferencesShouldResolveAndAllowPlaceholderBaseUrl() {
        try (ExcelReader excelReader = new ExcelReader(FINAL_TEMPLATE.toString())) {
            DataReader dataReader = new DataReader(excelReader);

            Assert.assertEquals(dataReader.resolveValue("CONFIG.BASE_URL", "1"), "file:///CHANGE_THIS_TO_LOCAL_HTML_OR_APP_URL");
            Assert.assertEquals(dataReader.resolveValue("LOGIN_DATA.USERNAME", "1"), "brs_admin");
            Assert.assertEquals(dataReader.resolveValue("LOGIN_DATA.PASSWORD", "1"), "brs123");
            Assert.assertEquals(dataReader.resolveValue("BOOKING_DATA.BOOKING_TITLE", "1"), "Weekly Meeting");
            Assert.assertEquals(dataReader.resolveValue("BOOKING_DATA.ROOM_NAME", "1"), "Meeting Room A");
            Assert.assertEquals(dataReader.resolveValue("BOOKING_DATA.EXPECTED_MESSAGE", "1"), "Booking created successfully");
            Assert.assertEquals(dataReader.resolveValue("BOOKING_DATA.BOOKING_ID", "3"), "BOOK-003");
        }
    }

    @Test
    public void finalTemplateObjectRepositoryShouldResolveWithCurrentColumnOrder() {
        try (ExcelReader excelReader = new ExcelReader(FINAL_TEMPLATE.toString())) {
            DataReader dataReader = new DataReader(excelReader);
            ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);

            for (Map.Entry<String, String> expectedObject : EXPECTED_OBJECTS.entrySet()) {
                String[] objectKey = expectedObject.getKey().split("::", -1);
                TestObject testObject = objectRepositoryReader.getObject(objectKey[0], objectKey[1]);
                Assert.assertEquals(testObject.getApplication(), objectKey[0]);
                Assert.assertEquals(testObject.getObjectName(), objectKey[1]);
                Assert.assertEquals(testObject.getXpath(), expectedObject.getValue());
            }

            Scenario localScenario = new Scenario("1", true, "Local Keyword Test", "Local keyword execution test", 2);
            ResolvedObject roomObject = objectRepositoryReader.resolveObject(
                    new TestStep("1", "Local keyword execution test", "Local Keyword Test", "Create Booking", "click", "btnRoomByName",
                            "BOOKING_DATA.ROOM_NAME", "BRS", "Select room", 12, 4),
                    localScenario
            );
            Assert.assertEquals(roomObject.getRawXpath(), "//button[contains(text(),'{ROOM_NAME}')]");
            Assert.assertEquals(roomObject.getResolvedXpath(), "//button[contains(text(),'Meeting Room A')]");

            Scenario cancelScenario = new Scenario("3", false, "Cancel Booking", "Cancel booking example", 4);
            ResolvedObject cancelObject = objectRepositoryReader.resolveObject(
                    new TestStep("3", "Cancel booking example", "Cancel Booking", "Cancel Booking", "click", "btnCancelBooking",
                            "BOOKING_DATA.BOOKING_ID", "BRS", "Cancel booking", 8, 1),
                    cancelScenario
            );
            Assert.assertEquals(cancelObject.getRawXpath(), "//button[@data-booking='{BOOKING_ID}']");
            Assert.assertEquals(cancelObject.getResolvedXpath(), "//button[@data-booking='BOOK-003']");
        }
    }

    private static void assertHeadersInOrder(Sheet sheet, List<String> expectedHeaders) {
        Assert.assertNotNull(sheet, "Expected sheet should exist.");
        DataFormatter formatter = new DataFormatter();
        Row headerRow = sheet.getRow(0);
        Assert.assertNotNull(headerRow, "Header row should exist in sheet: " + sheet.getSheetName());

        for (int columnIndex = 0; columnIndex < expectedHeaders.size(); columnIndex++) {
            Assert.assertEquals(
                    readCell(formatter, headerRow, columnIndex),
                    expectedHeaders.get(columnIndex),
                    "Unexpected header. Sheet = " + sheet.getSheetName() + ", column = " + (columnIndex + 1)
            );
        }
    }

    private static void assertStepOrderAndInheritedApplication(List<TestCaseBlock> testCases) {
        for (TestCaseBlock testCaseBlock : testCases) {
            int previousExcelRow = testCaseBlock.getExcelRowNumber();
            for (int stepIndex = 0; stepIndex < testCaseBlock.getSteps().size(); stepIndex++) {
                TestStep step = testCaseBlock.getSteps().get(stepIndex);
                Assert.assertEquals(step.getStepOrder(), stepIndex + 1);
                Assert.assertTrue(step.getExcelRowNumber() > previousExcelRow, "Step order should follow Excel row order.");
                Assert.assertEquals(step.getApplication(), testCaseBlock.getApplication(), "Blank step Application should inherit from parent testcase.");
                previousExcelRow = step.getExcelRowNumber();
            }
        }
    }

    private static void assertWorkbookDoesNotContainRemovedColumn(Workbook workbook) {
        DataFormatter formatter = new DataFormatter();
        for (Sheet sheet : workbook) {
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                continue;
            }

            for (Cell cell : headerRow) {
                Assert.assertNotEquals(formatter.formatCellValue(cell).trim(), "DATA_ROW");
            }
        }
    }

    private static void assertScenarioValuesUseDotReferences(Workbook workbook) {
        DataFormatter formatter = new DataFormatter();
        Set<String> discoveredReferences = new LinkedHashSet<>();

        for (String sheetName : SCENARIO_SHEETS) {
            Sheet sheet = workbook.getSheet(sheetName);
            Assert.assertNotNull(sheet, "Scenario sheet should exist: " + sheetName);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String value = readCell(formatter, row, 4);
                Assert.assertFalse(
                        value.contains("[") || value.contains("]"),
                        "Scenario Value cells should use dot notation. Sheet = " + sheetName + ", row = " + (rowIndex + 1)
                );

                if (value.chars().filter(character -> character == '.').count() == 1) {
                    String[] referenceParts = value.split("\\.", -1);
                    Assert.assertNotNull(workbook.getSheet(referenceParts[0]), "Referenced data sheet should exist: " + referenceParts[0]);
                    assertHeaderExists(workbook.getSheet(referenceParts[0]), referenceParts[1]);
                    discoveredReferences.add(value);
                }
            }
        }

        Assert.assertTrue(discoveredReferences.containsAll(REQUIRED_DATA_REFERENCES), "Final template should include the required data references.");
    }

    private static void assertHeaderExists(Sheet sheet, String expectedHeader) {
        DataFormatter formatter = new DataFormatter();
        Row headerRow = sheet.getRow(0);
        Assert.assertNotNull(headerRow, "Header row should exist in sheet: " + sheet.getSheetName());

        for (Cell cell : headerRow) {
            if (expectedHeader.equalsIgnoreCase(formatter.formatCellValue(cell).trim())) {
                return;
            }
        }

        Assert.fail("Header should exist. Sheet = " + sheet.getSheetName() + ", header = " + expectedHeader);
    }

    private static String readCell(DataFormatter formatter, Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }
}
