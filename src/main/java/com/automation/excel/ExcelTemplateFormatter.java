package com.automation.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public final class ExcelTemplateFormatter {

    private static final int EXCEL_CHARACTER_WIDTH_UNIT = 256;
    private static final int MIN_COLUMN_WIDTH = 10 * EXCEL_CHARACTER_WIDTH_UNIT;
    private static final int MAX_COLUMN_WIDTH = 45 * EXCEL_CHARACTER_WIDTH_UNIT;
    private static final int COLUMN_PADDING = 512;

    private ExcelTemplateFormatter() {
    }

    public static void applyReadableLayout(Workbook workbook) {
        if (workbook == null) {
            throw new IllegalArgumentException("Workbook must not be null.");
        }

        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            autoFitColumns(workbook.getSheetAt(sheetIndex));
        }
    }

    public static void autoFitColumns(Sheet sheet) {
        if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
            return;
        }

        sheet.createFreezePane(0, 1);

        Row headerRow = sheet.getRow(0);
        if (headerRow == null || headerRow.getLastCellNum() < 0) {
            return;
        }

        for (int columnIndex = 0; columnIndex < headerRow.getLastCellNum(); columnIndex++) {
            sheet.autoSizeColumn(columnIndex);

            int currentWidth = sheet.getColumnWidth(columnIndex);
            int paddedWidth = currentWidth + COLUMN_PADDING;
            int finalWidth = Math.max(MIN_COLUMN_WIDTH, Math.min(paddedWidth, MAX_COLUMN_WIDTH));

            sheet.setColumnWidth(columnIndex, finalWidth);
        }
    }
}
