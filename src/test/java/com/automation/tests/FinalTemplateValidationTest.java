package com.automation.tests;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FinalTemplateValidationTest {

    private static final Path FINAL_TEMPLATE = Path.of(
            "src",
            "test",
            "resources",
            "testdata",
            "Final Excel Template.xlsx"
    );

    private static final List<String> SCENARIO_SHEETS = List.of(
            "Local Keyword Test",
            "Create New Booking",
            "Cancel Booking"
    );

    private static final Map<String, List<List<String>>> EXPECTED_SHEETS = createExpectedSheets();

    @Test
    public void finalTemplateShouldExist() {
        Assert.assertTrue(Files.exists(FINAL_TEMPLATE), "Final Excel template should exist.");
        Assert.assertTrue(Files.isRegularFile(FINAL_TEMPLATE), "Final Excel template should be a file.");
    }

    @Test
    public void finalTemplateShouldMatchExpectedWorkbookContract() throws IOException {
        try (InputStream inputStream = Files.newInputStream(FINAL_TEMPLATE);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Assert.assertEquals(workbook.getNumberOfSheets(), EXPECTED_SHEETS.size());

            int sheetIndex = 0;
            for (Map.Entry<String, List<List<String>>> expectedSheet : EXPECTED_SHEETS.entrySet()) {
                Assert.assertEquals(workbook.getSheetName(sheetIndex), expectedSheet.getKey());
                assertSheetMatches(workbook.getSheet(expectedSheet.getKey()), expectedSheet.getValue());
                sheetIndex++;
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

    private static void assertSheetMatches(Sheet sheet, List<List<String>> expectedRows) {
        Assert.assertNotNull(sheet, "Expected sheet should exist.");
        Assert.assertEquals(sheet.getLastRowNum(), expectedRows.size() - 1, "Unexpected row count for " + sheet.getSheetName());

        DataFormatter formatter = new DataFormatter();
        for (int rowIndex = 0; rowIndex < expectedRows.size(); rowIndex++) {
            List<String> expectedRow = expectedRows.get(rowIndex);
            Row row = sheet.getRow(rowIndex);
            Assert.assertNotNull(row, "Expected row should exist. Sheet = " + sheet.getSheetName() + ", row = " + (rowIndex + 1));

            for (int columnIndex = 0; columnIndex < expectedRow.size(); columnIndex++) {
                String actual = readCell(formatter, row, columnIndex);
                Assert.assertEquals(
                        actual,
                        expectedRow.get(columnIndex),
                        "Unexpected cell value. Sheet = " + sheet.getSheetName()
                                + ", row = " + (rowIndex + 1)
                                + ", column = " + (columnIndex + 1)
                );
            }
        }
    }

    private static void assertWorkbookDoesNotContainRemovedColumn(Workbook workbook) {
        DataFormatter formatter = new DataFormatter();
        for (Sheet sheet : workbook) {
            for (Row row : sheet) {
                for (Cell cell : row) {
                    Assert.assertNotEquals(formatter.formatCellValue(cell).trim(), "DATA_ROW");
                }
            }
        }
    }

    private static void assertScenarioValuesUseDotReferences(Workbook workbook) {
        DataFormatter formatter = new DataFormatter();
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
            }
        }
    }

    private static String readCell(DataFormatter formatter, Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private static Map<String, List<List<String>>> createExpectedSheets() {
        Map<String, List<List<String>>> sheets = new LinkedHashMap<>();

        List<String> scenarioHeaders = List.of("Testcase", "Run", "Function", "Object", "Value", "Application", "Description");

        sheets.put("SCENARIOS", List.of(
                List.of("NO", "RUN", "ACTION", "SCENARIOS"),
                List.of("1", "Y", "Local Keyword Test", "Local keyword execution test"),
                List.of("2", "N", "Create New Booking", "Create booking room example"),
                List.of("3", "N", "Cancel Booking", "Cancel booking example")
        ));

        sheets.put("Local Keyword Test", List.of(
                scenarioHeaders,
                List.of("Open Local Page", "Yes", "", "", "", "BRS", "Open local test page"),
                List.of("", "", "openUrl", "", "CONFIG.BASE_URL", "", "Open local HTML"),
                List.of("Login BRS", "Yes", "", "", "", "BRS", "Login test"),
                List.of("", "", "input", "txtUsername", "LOGIN_DATA.USERNAME", "", "Input username"),
                List.of("", "", "input", "txtPassword", "LOGIN_DATA.PASSWORD", "", "Input password"),
                List.of("", "", "click", "btnLogin", "", "", "Click login"),
                List.of("", "", "verifyDisplayed", "lblDashboard", "", "", "Verify dashboard"),
                List.of("Create Booking", "Yes", "", "", "", "BRS", "Create booking test"),
                List.of("", "", "input", "txtBookingTitle", "BOOKING_DATA.BOOKING_TITLE", "", "Input booking title"),
                List.of("", "", "screenshot", "", "After input title", "", "Capture form after title"),
                List.of("", "", "click", "btnRoomByName", "BOOKING_DATA.ROOM_NAME", "", "Select room"),
                List.of("", "", "screenshot", "", "After select room", "", "Capture selected room"),
                List.of("", "", "verifyText", "lblSuccessMessage", "BOOKING_DATA.EXPECTED_MESSAGE", "", "Verify success"),
                List.of("", "", "screenshot", "", "After submit", "", "Capture submit result")
        ));

        sheets.put("Create New Booking", List.of(
                scenarioHeaders,
                List.of("Login BRS", "Yes", "", "", "", "BRS", "Login to BRS"),
                List.of("", "", "openUrl", "", "CONFIG.BASE_URL", "", "Open application"),
                List.of("", "", "input", "txtUsername", "LOGIN_DATA.USERNAME", "", "Input username"),
                List.of("", "", "input", "txtPassword", "LOGIN_DATA.PASSWORD", "", "Input password"),
                List.of("", "", "click", "btnLogin", "", "", "Click login"),
                List.of("", "", "verifyDisplayed", "lblDashboard", "", "", "Verify dashboard"),
                List.of("Create Booking", "Yes", "", "", "", "BRS", "Create booking"),
                List.of("", "", "input", "txtBookingTitle", "BOOKING_DATA.BOOKING_TITLE", "", "Input booking title"),
                List.of("", "", "click", "btnRoomByName", "BOOKING_DATA.ROOM_NAME", "", "Select room"),
                List.of("", "", "verifyText", "lblSuccessMessage", "BOOKING_DATA.EXPECTED_MESSAGE", "", "Verify success")
        ));

        sheets.put("Cancel Booking", List.of(
                scenarioHeaders,
                List.of("Login BRS", "Yes", "", "", "", "BRS", "Login to BRS"),
                List.of("", "", "openUrl", "", "CONFIG.BASE_URL", "", "Open application"),
                List.of("", "", "input", "txtUsername", "LOGIN_DATA.USERNAME", "", "Input username"),
                List.of("", "", "input", "txtPassword", "LOGIN_DATA.PASSWORD", "", "Input password"),
                List.of("", "", "click", "btnLogin", "", "", "Click login"),
                List.of("Cancel Booking", "Yes", "", "", "", "BRS", "Cancel booking"),
                List.of("", "", "click", "btnCancelBooking", "BOOKING_DATA.BOOKING_ID", "", "Cancel booking"),
                List.of("", "", "verifyText", "lblSuccessMessage", "BOOKING_DATA.EXPECTED_MESSAGE", "", "Verify success")
        ));

        sheets.put("CONFIG", List.of(
                List.of("NO", "BASE_URL"),
                List.of("1", "file:///CHANGE_THIS_TO_LOCAL_HTML_OR_APP_URL"),
                List.of("2", "https://example-booking-room-system.local"),
                List.of("3", "https://example-booking-room-system.local")
        ));

        sheets.put("LOGIN_DATA", List.of(
                List.of("NO", "USERNAME", "PASSWORD"),
                List.of("1", "brs_admin", "brs123"),
                List.of("2", "brs_user", "brs456"),
                List.of("3", "brs_user_cancel", "brs789")
        ));

        sheets.put("BOOKING_DATA", List.of(
                List.of("NO", "BOOKING_TITLE", "ROOM_NAME", "EXPECTED_MESSAGE", "BOOKING_ID"),
                List.of("1", "Weekly Meeting", "Meeting Room A", "Booking created successfully", "BOOK-001"),
                List.of("2", "Daily Standup", "Meeting Room B", "Booking created successfully", "BOOK-002"),
                List.of("3", "Cancel Test Booking", "Meeting Room A", "Booking cancelled successfully", "BOOK-003")
        ));

        sheets.put("OBJECT_REPOSITORY", List.of(
                List.of("Application", "Object", "XPath", "Description"),
                List.of("BRS", "txtUsername", "//input[@id='username']", "Username input"),
                List.of("BRS", "txtPassword", "//input[@id='password']", "Password input"),
                List.of("BRS", "btnLogin", "//button[@id='loginButton']", "Login button"),
                List.of("BRS", "lblDashboard", "//h1[@id='dashboard']", "Dashboard title"),
                List.of("BRS", "txtBookingTitle", "//input[@id='bookingTitle']", "Booking title input"),
                List.of("BRS", "btnRoomByName", "//button[contains(text(),'{ROOM_NAME}')]", "Dynamic room button"),
                List.of("BRS", "lblSuccessMessage", "//div[@id='message']", "Success message"),
                List.of("BRS", "btnCancelBooking", "//button[@data-booking='{BOOKING_ID}']", "Dynamic cancel booking button"),
                List.of("HRIS", "txtUsername", "//input[@id='employeeId']", "HRIS username input"),
                List.of("HRIS", "txtPassword", "//input[@id='password']", "HRIS password input"),
                List.of("HRIS", "btnLogin", "//button[@id='loginButton']", "HRIS login button")
        ));

        return sheets;
    }
}
