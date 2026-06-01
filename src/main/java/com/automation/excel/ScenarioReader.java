package com.automation.excel;

import com.automation.models.Scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScenarioReader {

    private static final String SCENARIOS_SHEET = "SCENARIOS";
    private static final String NO_COLUMN = "NO";
    private static final String RUN_COLUMN = "RUN";
    private static final String ACTION_COLUMN = "ACTION";
    private static final String SCENARIOS_COLUMN = "SCENARIOS";
    private static final String DATA_ROW_COLUMN = "DATA_ROW";
    private static final String ALLOWED_RUN_VALUES = "Y, YES, TRUE, N, NO, FALSE, blank";

    private final ExcelReader excelReader;

    public ScenarioReader(ExcelReader excelReader) {
        if (excelReader == null) {
            throw new IllegalArgumentException("ExcelReader must not be null.");
        }
        this.excelReader = excelReader;
    }

    public List<Scenario> getActiveScenarios() {
        return getAllScenarios().stream()
                .filter(Scenario::isRun)
                .toList();
    }

    public List<Scenario> getAllScenarios() {
        validateRequiredHeaders();

        List<Scenario> scenarios = new ArrayList<>();
        int lastRowNumber = excelReader.getLastRowNumber(SCENARIOS_SHEET);
        for (int rowIndex = 1; rowIndex <= lastRowNumber; rowIndex++) {
            if (!isScenarioRowBlank(rowIndex)) {
                scenarios.add(toScenario(rowIndex));
            }
        }
        return scenarios;
    }

    public void validateScenarios() {
        getAllScenarios().stream()
                .filter(Scenario::isRun)
                .forEach(this::validateActiveScenario);
    }

    private void validateRequiredHeaders() {
        if (!excelReader.isSheetExists(SCENARIOS_SHEET)) {
            throw new IllegalArgumentException("Sheet not found: " + SCENARIOS_SHEET);
        }

        excelReader.findColumnIndex(SCENARIOS_SHEET, NO_COLUMN);
        excelReader.findColumnIndex(SCENARIOS_SHEET, RUN_COLUMN);
        excelReader.findColumnIndex(SCENARIOS_SHEET, ACTION_COLUMN);
        excelReader.findColumnIndex(SCENARIOS_SHEET, SCENARIOS_COLUMN);
        excelReader.findColumnIndex(SCENARIOS_SHEET, DATA_ROW_COLUMN);
    }

    private Scenario toScenario(int rowIndex) {
        String runValue = readCell(rowIndex, RUN_COLUMN);
        boolean run = parseRunValue(runValue, toExcelRowNumber(rowIndex));
        Scenario scenario = new Scenario(
                readCell(rowIndex, NO_COLUMN),
                run,
                readCell(rowIndex, ACTION_COLUMN),
                readCell(rowIndex, SCENARIOS_COLUMN),
                readCell(rowIndex, DATA_ROW_COLUMN),
                toExcelRowNumber(rowIndex)
        );

        if (scenario.isRun()) {
            validateActiveScenario(scenario);
        }
        return scenario;
    }

    private boolean parseRunValue(String runValue, int excelRowNumber) {
        String normalizedValue = normalize(runValue);
        if (normalizedValue.isEmpty() || "N".equals(normalizedValue) || "NO".equals(normalizedValue) || "FALSE".equals(normalizedValue)) {
            return false;
        }
        if ("Y".equals(normalizedValue) || "YES".equals(normalizedValue) || "TRUE".equals(normalizedValue)) {
            return true;
        }
        throw new IllegalArgumentException("Invalid RUN value '" + runValue + "' in SCENARIOS row " + excelRowNumber + ". Allowed values: " + ALLOWED_RUN_VALUES + ".");
    }

    private void validateActiveScenario(Scenario scenario) {
        if (scenario.getAction().isBlank()) {
            throw new IllegalArgumentException("ACTION is required for active scenario at SCENARIOS row " + scenario.getExcelRowNumber() + ".");
        }
        if (scenario.getScenarioName().isBlank()) {
            throw new IllegalArgumentException("SCENARIOS description is required for active scenario at SCENARIOS row " + scenario.getExcelRowNumber() + ".");
        }
        if (scenario.getDataRow().isBlank()) {
            throw new IllegalArgumentException("DATA_ROW is required for active scenario at SCENARIOS row " + scenario.getExcelRowNumber() + ".");
        }
        if (!excelReader.isSheetExists(scenario.getAction())) {
            throw new IllegalArgumentException("Scenario sheet not found: " + scenario.getAction() + ". Referenced by SCENARIOS row " + scenario.getExcelRowNumber() + ".");
        }
    }

    private boolean isScenarioRowBlank(int rowIndex) {
        return readCell(rowIndex, NO_COLUMN).isBlank()
                && readCell(rowIndex, RUN_COLUMN).isBlank()
                && readCell(rowIndex, ACTION_COLUMN).isBlank()
                && readCell(rowIndex, SCENARIOS_COLUMN).isBlank()
                && readCell(rowIndex, DATA_ROW_COLUMN).isBlank();
    }

    private String readCell(int rowIndex, String columnName) {
        int columnIndex = excelReader.findColumnIndex(SCENARIOS_SHEET, columnName);
        try {
            if (columnIndex >= excelReader.getColumnCount(SCENARIOS_SHEET, rowIndex)) {
                return "";
            }
            return excelReader.getCellValue(SCENARIOS_SHEET, rowIndex, columnIndex).trim();
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("Row not found: row " + rowIndex + " in sheet " + SCENARIOS_SHEET)) {
                return "";
            }
            throw exception;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static int toExcelRowNumber(int rowIndex) {
        return rowIndex + 1;
    }
}
