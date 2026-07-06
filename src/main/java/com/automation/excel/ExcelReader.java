package com.automation.excel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExcelReader implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger(ExcelReader.class);
    private static final int HEADER_ROW_INDEX = 0;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Path filePath;
    private final InputStream inputStream;
    private final Workbook workbook;
    private final FormulaEvaluator formulaEvaluator;
    private boolean closed;

    public ExcelReader(String filePath) {
        this.filePath = validateFilePath(filePath);

        try {
            this.inputStream = Files.newInputStream(this.filePath);
            this.workbook = new XSSFWorkbook(this.inputStream);
            this.formulaEvaluator = this.workbook.getCreationHelper().createFormulaEvaluator();
            LOGGER.info("Excel file loaded: {}", this.filePath.toAbsolutePath());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to open Excel workbook: " + this.filePath.toAbsolutePath(), exception);
        }
    }

    public boolean isSheetExists(String sheetName) {
        ensureWorkbookOpen();
        return sheetName != null && workbook.getSheet(sheetName) != null;
    }

    public int getLastRowNumber(String sheetName) {
        return getSheetOrThrow(sheetName).getLastRowNum();
    }

    public int getPhysicalRowCount(String sheetName) {
        return getSheetOrThrow(sheetName).getPhysicalNumberOfRows();
    }

    public int getColumnCount(String sheetName, int rowIndex) {
        Sheet sheet = getSheetOrThrow(sheetName);
        Row row = getRowOrThrow(sheet, sheetName, rowIndex);
        short lastCellNumber = row.getLastCellNum();
        return lastCellNumber < 0 ? 0 : lastCellNumber;
    }

    public String getCellValue(String sheetName, int rowIndex, int columnIndex) {
        Sheet sheet = getSheetOrThrow(sheetName);
        Row row = getRowOrThrow(sheet, sheetName, rowIndex);
        validateColumnIndex(row, sheetName, columnIndex);

        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }
        return getCellValue(cell, sheetName, rowIndex, columnIndex);
    }

    public String getCellValuePreservingFormulaHeaderReference(String sheetName, int rowIndex, int columnIndex) {
        Sheet sheet = getSheetOrThrow(sheetName);
        Row row = getRowOrThrow(sheet, sheetName, rowIndex);
        validateColumnIndex(row, sheetName, columnIndex);

        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.FORMULA) {
            return FormulaHeaderReference.parseCellFormula(cell.getCellFormula())
                    .map(FormulaHeaderReference::rawFormula)
                    .orElseGet(() -> getCellValue(cell, sheetName, rowIndex, columnIndex));
        }
        return getCellValue(cell, sheetName, rowIndex, columnIndex);
    }

    public String getCellValue(String sheetName, int rowIndex, String columnName) {
        int columnIndex = findColumnIndex(sheetName, columnName);
        return getCellValue(sheetName, rowIndex, columnIndex);
    }

    public Map<String, String> getRowDataByHeader(String sheetName, int rowIndex) {
        Sheet sheet = getSheetOrThrow(sheetName);
        getRowOrThrow(sheet, sheetName, rowIndex);

        Map<String, String> rowData = new LinkedHashMap<>();
        int columnCount = getColumnCount(sheetName, HEADER_ROW_INDEX);
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            String header = getCellValue(sheetName, HEADER_ROW_INDEX, columnIndex).trim();
            if (!header.isEmpty()) {
                rowData.put(header, getCellValue(sheetName, rowIndex, columnIndex));
            }
        }
        return rowData;
    }

    public List<Map<String, String>> getAllRowsDataByHeader(String sheetName) {
        Sheet sheet = getSheetOrThrow(sheetName);
        int lastRowNumber = sheet.getLastRowNum();
        List<Map<String, String>> allRows = new ArrayList<>();

        for (int rowIndex = HEADER_ROW_INDEX + 1; rowIndex <= lastRowNumber; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null && !isRowEmpty(sheetName, rowIndex)) {
                allRows.add(getRowDataByHeader(sheetName, rowIndex));
            }
        }
        return allRows;
    }

    public int findColumnIndex(String sheetName, String columnName) {
        String expectedHeader = normalizeHeader(columnName);
        if (expectedHeader.isEmpty()) {
            throw new IllegalArgumentException("Header name must not be null or blank.");
        }

        int columnCount = getColumnCount(sheetName, HEADER_ROW_INDEX);
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            String actualHeader = normalizeHeader(getCellValue(sheetName, HEADER_ROW_INDEX, columnIndex));
            if (expectedHeader.equals(actualHeader)) {
                return columnIndex;
            }
        }

        throw new IllegalArgumentException("Header not found: " + columnName.trim() + " in sheet " + sheetName);
    }

    public int findRowIndexByCellValue(String sheetName, String columnName, String expectedValue) {
        Sheet sheet = getSheetOrThrow(sheetName);
        int columnIndex = findColumnIndex(sheetName, columnName);
        String expected = expectedValue == null ? "" : expectedValue.trim();

        for (int rowIndex = HEADER_ROW_INDEX + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null && columnIndex < getColumnCount(sheetName, rowIndex)) {
                String actual = getCellValue(sheetName, rowIndex, columnIndex).trim();
                if (expected.equals(actual)) {
                    return rowIndex;
                }
            }
        }

        throw new IllegalArgumentException("Row not found: value '" + expected + "' in header " + columnName + " in sheet " + sheetName);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        IOException closeException = null;
        try {
            workbook.close();
        } catch (IOException exception) {
            closeException = exception;
        }

        try {
            inputStream.close();
        } catch (IOException exception) {
            if (closeException == null) {
                closeException = exception;
            } else {
                closeException.addSuppressed(exception);
            }
        }

        closed = true;
        LOGGER.info("Excel file closed: {}", filePath.toAbsolutePath());

        if (closeException != null) {
            throw new IllegalStateException("Unable to close Excel workbook: " + filePath.toAbsolutePath(), closeException);
        }
    }

    private static Path validateFilePath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Excel file path must not be null or blank.");
        }

        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Excel file not found: " + path.toAbsolutePath());
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Excel path is not a file: " + path.toAbsolutePath());
        }
        if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Unsupported Excel file type. Only .xlsx is supported: " + path.toAbsolutePath());
        }
        return path;
    }

    private Sheet getSheetOrThrow(String sheetName) {
        ensureWorkbookOpen();
        if (sheetName == null || sheetName.isBlank()) {
            throw new IllegalArgumentException("Sheet name must not be null or blank.");
        }

        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException("Sheet not found: " + sheetName);
        }
        return sheet;
    }

    private Row getRowOrThrow(Sheet sheet, String sheetName, int rowIndex) {
        if (rowIndex < 0) {
            throw new IllegalArgumentException("Row index must not be negative: row " + rowIndex + " in sheet " + sheetName);
        }

        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            throw new IllegalArgumentException("Row not found: row " + rowIndex + " in sheet " + sheetName);
        }
        return row;
    }

    private void validateColumnIndex(Row row, String sheetName, int columnIndex) {
        if (columnIndex < 0) {
            throw new IllegalArgumentException("Column index must not be negative: column " + columnIndex + " in sheet " + sheetName);
        }

        int columnCount = row.getLastCellNum();
        if (columnCount < 0 || columnIndex >= columnCount) {
            throw new IllegalArgumentException("Column index out of range: column " + columnIndex + " in sheet " + sheetName);
        }
    }

    private String getCellValue(Cell cell, String sheetName, int rowIndex, int columnIndex) {
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            return getFormulaCellValue(cell, sheetName, rowIndex, columnIndex);
        }

        return switch (cellType) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> getNumericCellValue(cell);
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case BLANK -> "";
            case ERROR -> getCellErrorMessage(cell.getErrorCellValue(), sheetName, rowIndex, columnIndex);
            default -> "";
        };
    }

    private String getFormulaCellValue(Cell cell, String sheetName, int rowIndex, int columnIndex) {
        try {
            CellValue cellValue = formulaEvaluator.evaluate(cell);
            if (cellValue == null) {
                return "";
            }

            return switch (cellValue.getCellType()) {
                case STRING -> cellValue.getStringValue();
                case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                        ? DateUtil.getLocalDateTime(cellValue.getNumberValue()).toLocalDate().format(DATE_FORMATTER)
                        : normalizeNumber(cellValue.getNumberValue());
                case BOOLEAN -> String.valueOf(cellValue.getBooleanValue());
                case BLANK -> "";
                case ERROR -> getCellErrorMessage(cellValue.getErrorValue(), sheetName, rowIndex, columnIndex);
                default -> "";
            };
        } catch (RuntimeException exception) {
            return "Formula evaluation failed in sheet " + sheetName + ", row " + rowIndex + ", column " + columnIndex + ": " + exception.getMessage();
        }
    }

    private String getNumericCellValue(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate().format(DATE_FORMATTER);
        }
        return normalizeNumber(cell.getNumericCellValue());
    }

    private String getCellErrorMessage(byte errorValue, String sheetName, int rowIndex, int columnIndex) {
        String errorText = FormulaError.forInt(errorValue).getString();
        return "Cell error in sheet " + sheetName + ", row " + rowIndex + ", column " + columnIndex + ": " + errorText;
    }

    private boolean isRowEmpty(String sheetName, int rowIndex) {
        int columnCount = getColumnCount(sheetName, HEADER_ROW_INDEX);
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            if (!getCellValue(sheetName, rowIndex, columnIndex).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeNumber(double number) {
        return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
    }

    private static String normalizeHeader(String header) {
        return header == null ? "" : header.trim().toLowerCase(Locale.ROOT);
    }

    private void ensureWorkbookOpen() {
        if (closed) {
            throw new IllegalStateException("Excel workbook is already closed: " + filePath.toAbsolutePath());
        }
    }
}
