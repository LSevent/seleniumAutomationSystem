package com.automation.tests.support;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ValidationWorkbookFactory {

    private ValidationWorkbookFactory() {
    }

    public static Path createWorkbook(Path workbookPath, SheetData... sheets) throws IOException {
        Files.createDirectories(workbookPath.getParent());
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

    public static SheetData sheet(String name, String[] headers, Object[][] rows) {
        return new SheetData(name, headers, rows);
    }

    public static SheetData scenarios(Object[][] rows) {
        return sheet("SCENARIOS", new String[]{"NO", "RUN", "ACTION", "SCENARIOS"}, rows);
    }

    public static SheetData scenarioSheet(String name, Object[][] rows) {
        return sheet(name, new String[]{"Testcase", "Run", "Function", "Object", "Value", "Application", "Description"}, rows);
    }

    public static SheetData objectRepository(Object[][] rows) {
        return sheet("OBJECT_REPOSITORY", new String[]{"Application", "Object", "XPath", "Description"}, rows);
    }

    public static SheetData loginData(Object[][] rows) {
        return sheet("LOGIN_DATA", new String[]{"NO", "USERNAME", "PASSWORD"}, rows);
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

    public record SheetData(String name, String[] headers, Object[][] rows) {
    }
}
