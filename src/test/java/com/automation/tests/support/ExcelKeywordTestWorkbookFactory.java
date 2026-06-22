package com.automation.tests.support;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ExcelKeywordTestWorkbookFactory {

    private ExcelKeywordTestWorkbookFactory() {
    }

    public static Path createWorkbook(Path workbookPath, String baseUrl) throws IOException {
        Files.createDirectories(workbookPath.getParent());
        try (Workbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(workbookPath)) {
            createSheet(
                    workbook,
                    "SCENARIOS",
                    new String[]{"NO", "RUN", "ACTION", "SCENARIOS"},
                    new Object[][]{
                            {1, "Y", "Local Keyword Test", "Local keyword execution test"},
                            {2, "N", "Inactive Keyword Test", "Inactive scenario should be skipped"}
                    }
            );
            createSheet(
                    workbook,
                    "Local Keyword Test",
                    new String[]{"Testcase", "Run", "Keyword", "Object", "Value", "Application", "Description"},
                    new Object[][]{
                            {"Open Local Page", "Yes", "", "", "", "BRS", "Open local test page"},
                            {"", "", "openUrl", "", "CONFIG.BASE_URL", "", "Open local HTML"},
                            {"Login BRS", "Yes", "", "", "", "BRS", "Login test"},
                            {"", "", "input", "txtUsername", "LOGIN_DATA.USERNAME", "", "Input username"},
                            {"", "", "input", "txtPassword", "LOGIN_DATA.PASSWORD", "", "Input password"},
                            {"", "", "click", "btnLogin", "", "", "Click login"},
                            {"", "", "verifyDisplayed", "lblDashboard", "", "", "Verify dashboard"},
                            {"Create Booking", "Yes", "", "", "", "BRS", "Create booking test"},
                            {"", "", "input", "txtBookingTitle", "BOOKING_DATA.BOOKING_TITLE", "", "Input booking title"},
                            {"", "", "screenshot", "", "After input title", "", "Capture form after title"},
                            {"", "", "click", "btnRoomByName", "BOOKING_DATA.ROOM_NAME", "", "Select room"},
                            {"", "", "screenshot", "", "After select room", "", "Capture selected room"},
                            {"", "", "verifyText", "lblSuccessMessage", "BOOKING_DATA.EXPECTED_MESSAGE", "", "Verify success"},
                            {"", "", "screenshot", "", "After submit", "", "Capture submit result"}
                    }
            );
            createSheet(
                    workbook,
                    "LOGIN_DATA",
                    new String[]{"NO", "USERNAME", "PASSWORD"},
                    new Object[][]{
                            {1, "brs_admin", "brs123"},
                            {2, "inactive_user", "inactive_password"}
                    }
            );
            createSheet(
                    workbook,
                    "BOOKING_DATA",
                    new String[]{"NO", "BOOKING_TITLE", "ROOM_NAME", "EXPECTED_MESSAGE"},
                    new Object[][]{
                            {1, "Weekly Meeting", "Meeting Room A", "Booking created successfully"},
                            {2, "Inactive Meeting", "Meeting Room B", "Inactive booking"}
                    }
            );
            createSheet(
                    workbook,
                    "CONFIG",
                    new String[]{"NO", "BASE_URL"},
                    new Object[][]{
                            {1, baseUrl},
                            {2, baseUrl}
                    }
            );
            createSheet(
                    workbook,
                    "OBJECT_REPOSITORY",
                    new String[]{"Application", "Object", "XPath", "Description"},
                    new Object[][]{
                            {"BRS", "txtUsername", "//input[@id='username']", "Username input"},
                            {"BRS", "txtPassword", "//input[@id='password']", "Password input"},
                            {"BRS", "btnLogin", "//button[@id='loginButton']", "Login button"},
                            {"BRS", "lblDashboard", "//h1[@id='dashboard']", "Dashboard title"},
                            {"BRS", "txtBookingTitle", "//input[@id='bookingTitle']", "Booking title input"},
                            {"BRS", "btnRoomByName", "//button[contains(text(),'{ROOM_NAME}')]", "Dynamic room button"},
                            {"BRS", "lblSuccessMessage", "//div[@id='message']", "Success message"}
                    }
            );
            workbook.write(outputStream);
        }
        return workbookPath;
    }

    public static Path createFailureWorkbook(Path workbookPath, String baseUrl) throws IOException {
        return createFailureWorkbook(workbookPath, baseUrl, 9, "Failing Keyword Test", "Failing keyword execution test", "Failing Step");
    }

    public static Path createFailureWorkbook(
            Path workbookPath,
            String baseUrl,
            int scenarioNo,
            String action,
            String scenarioDescription,
            String testcaseName
    ) throws IOException {
        Files.createDirectories(workbookPath.getParent());
        try (Workbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(workbookPath)) {
            createSheet(
                    workbook,
                    "SCENARIOS",
                    new String[]{"NO", "RUN", "ACTION", "SCENARIOS"},
                    new Object[][]{
                            {scenarioNo, "Y", action, scenarioDescription}
                    }
            );
            createSheet(
                    workbook,
                    action,
                    new String[]{"Testcase", "Run", "Keyword", "Object", "Value", "Application", "Description"},
                    new Object[][]{
                            {"Open Local Page", "Yes", "", "", "", "BRS", "Open local test page"},
                            {"", "", "openUrl", "", "CONFIG.BASE_URL", "", "Open local HTML"},
                            {testcaseName, "Yes", "", "", "", "BRS", "Failing testcase"},
                            {"", "", "unknownKeyword", "", "", "", "Unsupported keyword"}
                    }
            );
            createSheet(
                    workbook,
                    "CONFIG",
                    new String[]{"NO", "BASE_URL"},
                    new Object[][]{
                            {scenarioNo, baseUrl}
                    }
            );
            createSheet(
                    workbook,
                    "OBJECT_REPOSITORY",
                    new String[]{"Application", "Object", "XPath", "Description"},
                    new Object[][]{
                            {"BRS", "btnLogin", "//button[@id='loginButton']", "Login button"}
                    }
            );
            workbook.write(outputStream);
        }
        return workbookPath;
    }

    private static void createSheet(Workbook workbook, String sheetName, String[] headers, Object[][] rows) {
        Sheet sheet = workbook.createSheet(sheetName);
        writeRow(sheet.createRow(0), headers);
        for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            writeRow(sheet.createRow(rowIndex + 1), rows[rowIndex]);
        }
    }

    private static void writeRow(Row row, Object[] values) {
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
