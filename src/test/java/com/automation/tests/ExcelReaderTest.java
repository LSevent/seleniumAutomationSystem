package com.automation.tests;

import com.automation.excel.ExcelReader;
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
import java.util.Map;

public class ExcelReaderTest {

    private static final Path SAMPLE_FILE = Path.of("target", "testdata", "Template Testing.xlsx");
    private static final String SHEET_NAME = "SCENARIOS";

    @BeforeClass
    public void createSampleWorkbook() throws IOException {
        Files.createDirectories(SAMPLE_FILE.getParent());

        try (Workbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(SAMPLE_FILE)) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("NO");
            header.createCell(1).setCellValue("RUN");
            header.createCell(2).setCellValue("ACTION");
            header.createCell(3).setCellValue("SCENARIOS");
            header.createCell(4).setCellValue("DATA_ROW");

            Row firstDataRow = sheet.createRow(1);
            firstDataRow.createCell(0).setCellValue(1);
            firstDataRow.createCell(1).setCellValue("Y");
            firstDataRow.createCell(2).setCellValue("Create New Booking");
            firstDataRow.createCell(3).setCellValue("Create booking room A");
            firstDataRow.createCell(4).setCellValue(1);

            Row secondDataRow = sheet.createRow(2);
            secondDataRow.createCell(0).setCellValue(2);
            secondDataRow.createCell(1).setCellValue("N");
            secondDataRow.createCell(2).setCellValue("Cancel Booking");
            secondDataRow.createCell(3).setCellValue("Cancel booking");
            secondDataRow.createCell(4).setCellValue(2);

            workbook.write(outputStream);
        }
    }

    @Test
    public void excelReaderShouldReadWorkbookData() {
        try (ExcelReader excelReader = new ExcelReader(SAMPLE_FILE.toString())) {
            Assert.assertTrue(excelReader.isSheetExists(SHEET_NAME), "SCENARIOS sheet should exist.");
            Assert.assertEquals(excelReader.getLastRowNumber(SHEET_NAME), 2);
            Assert.assertEquals(excelReader.getPhysicalRowCount(SHEET_NAME), 3);
            Assert.assertEquals(excelReader.getColumnCount(SHEET_NAME, 0), 5);

            Assert.assertEquals(excelReader.getCellValue(SHEET_NAME, 0, 1), "RUN");
            Assert.assertEquals(excelReader.getCellValue(SHEET_NAME, 1, 0), "1");
            Assert.assertEquals(excelReader.getCellValue(SHEET_NAME, 1, " run "), "Y");

            Map<String, String> firstRowData = excelReader.getRowDataByHeader(SHEET_NAME, 1);
            Assert.assertEquals(firstRowData.get("ACTION"), "Create New Booking");
            Assert.assertEquals(firstRowData.get("SCENARIOS"), "Create booking room A");
            Assert.assertEquals(firstRowData.get("DATA_ROW"), "1");

            List<Map<String, String>> allRows = excelReader.getAllRowsDataByHeader(SHEET_NAME);
            Assert.assertEquals(allRows.size(), 2);
            Assert.assertEquals(allRows.get(1).get("ACTION"), "Cancel Booking");
            Assert.assertEquals(allRows.get(1).get("DATA_ROW"), "2");

            Assert.assertEquals(excelReader.findColumnIndex(SHEET_NAME, " Run "), 1);
            Assert.assertEquals(excelReader.findRowIndexByCellValue(SHEET_NAME, "RUN", "N"), 2);
        }
    }

    @Test
    public void missingSheetShouldThrowClearError() {
        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(SAMPLE_FILE.toString())) {
                excelReader.getCellValue("MISSING", 1, 0);
            }
        });

        Assert.assertTrue(exception.getMessage().contains("Sheet not found: MISSING"));
    }

    @Test
    public void missingHeaderShouldThrowClearError() {
        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(SAMPLE_FILE.toString())) {
                excelReader.getCellValue(SHEET_NAME, 1, "UNKNOWN");
            }
        });

        Assert.assertTrue(exception.getMessage().contains("Header not found: UNKNOWN in sheet SCENARIOS"));
    }
}
