package com.automation.excel;

import com.automation.exceptions.FrameworkException;
import com.automation.models.DataReference;
import com.automation.models.Scenario;

import java.util.Map;

public class DataReader {

    private static final String DATA_KEY_COLUMN = "NO";
    private static final String REFERENCE_FORMAT = "SHEET_NAME.COLUMN_NAME";

    private final ExcelReader excelReader;

    public DataReader(ExcelReader excelReader) {
        if (excelReader == null) {
            throw new IllegalArgumentException("ExcelReader must not be null.");
        }
        this.excelReader = excelReader;
    }

    public boolean isDataReference(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String trimmedValue = value.trim();
        if (containsBracketNotation(trimmedValue)) {
            throw invalidReferenceFormat(trimmedValue);
        }

        long dotCount = trimmedValue.chars().filter(character -> character == '.').count();
        if (dotCount == 0) {
            return false;
        }
        if (dotCount > 1) {
            throw invalidReferenceFormat(trimmedValue);
        }

        parseReference(trimmedValue);
        return true;
    }

    public DataReference parseReference(String value) {
        String rawReference = value == null ? "" : value.trim();
        if (rawReference.isEmpty() || containsBracketNotation(rawReference)) {
            throw invalidReferenceFormat(rawReference);
        }

        long dotCount = rawReference.chars().filter(character -> character == '.').count();
        if (dotCount != 1) {
            throw invalidReferenceFormat(rawReference);
        }

        String[] referenceParts = rawReference.split("\\.", -1);
        String sheetName = referenceParts[0].trim();
        String columnName = referenceParts[1].trim();

        if (sheetName.isBlank()) {
            throw new FrameworkException("Invalid data reference format: " + rawReference + ". Sheet name is required.");
        }
        if (columnName.isBlank()) {
            throw new FrameworkException("Invalid data reference format: " + sheetName + ". Column name is required.");
        }

        return new DataReference(rawReference, sheetName, columnName);
    }

    public String resolveValue(String rawValue, String scenarioNo) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }
        if (!isDataReference(rawValue)) {
            return rawValue;
        }

        DataReference dataReference = parseReference(rawValue);
        if (scenarioNo == null || scenarioNo.isBlank()) {
            throw new FrameworkException("Scenario NO is required to resolve data reference " + dataReference.getRawReference() + ".");
        }
        if (!excelReader.isSheetExists(dataReference.getSheetName())) {
            throw new FrameworkException("Data sheet not found: " + dataReference.getSheetName() + ". Referenced by value " + dataReference.getRawReference() + ".");
        }

        Map<String, String> dataRow = getDataRow(dataReference.getSheetName(), scenarioNo);
        int columnIndex = findRequiredColumnIndex(dataReference.getSheetName(), dataReference.getColumnName());
        String actualHeader = excelReader.getCellValue(dataReference.getSheetName(), 0, columnIndex).trim();

        return dataRow.getOrDefault(actualHeader, "");
    }

    public String resolveValue(String rawValue, Scenario scenario) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }
        if (!isDataReference(rawValue)) {
            return rawValue;
        }
        if (scenario == null) {
            throw new FrameworkException("Scenario is required to resolve data reference " + rawValue.trim() + ".");
        }
        return resolveValue(rawValue, scenario.getNo());
    }

    public Map<String, String> getDataRow(String sheetName, String scenarioNo) {
        if (sheetName == null || sheetName.isBlank()) {
            throw new FrameworkException("Data sheet name must not be null or blank.");
        }
        if (scenarioNo == null || scenarioNo.isBlank()) {
            throw new FrameworkException("Scenario NO is required to resolve data sheet " + sheetName.trim() + ".");
        }

        String dataSheetName = sheetName.trim();
        if (!excelReader.isSheetExists(dataSheetName)) {
            throw new FrameworkException("Data sheet not found: " + dataSheetName + ".");
        }
        findRequiredColumnIndex(dataSheetName, DATA_KEY_COLUMN);

        return excelReader.getAllRowsDataByHeader(dataSheetName).stream()
                .filter(rowData -> scenarioNo.trim().equals(rowData.getOrDefault(DATA_KEY_COLUMN, "").trim()))
                .findFirst()
                .orElseThrow(() -> new FrameworkException("Data row not found in sheet " + dataSheetName + " for NO = " + scenarioNo.trim() + "."));
    }

    private boolean containsBracketNotation(String value) {
        return value.contains("[") || value.contains("]");
    }

    private IllegalArgumentException invalidReferenceFormat(String rawReference) {
        return new FrameworkException("Invalid data reference format: " + rawReference + ". Expected format: " + REFERENCE_FORMAT + ".");
    }

    private int findRequiredColumnIndex(String sheetName, String columnName) {
        try {
            return excelReader.findColumnIndex(sheetName, columnName);
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("Header not found:")) {
                throw new FrameworkException("Header not found: " + columnName + " in sheet " + sheetName + ".", exception);
            }
            throw exception;
        }
    }
}
