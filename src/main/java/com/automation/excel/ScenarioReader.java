package com.automation.excel;

import com.automation.exceptions.FrameworkException;
import com.automation.models.Scenario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScenarioReader {

    private static final String SCENARIOS_SHEET = "SCENARIOS";
    private static final String NO_COLUMN = "NO";
    private static final String RUN_COLUMN = "RUN";
    private static final String ACTION_COLUMN = "ACTION";
    private static final String SCENARIOS_COLUMN = "SCENARIOS";
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
        validateUniqueActiveScenarioIdentities(scenarios);
        validateUniqueScenarioNumbers(scenarios);
        return scenarios;
    }

    public void validateScenarios() {
        getAllScenarios().stream()
                .filter(Scenario::isRun)
                .forEach(this::validateActiveScenario);
    }

    private void validateRequiredHeaders() {
        if (!excelReader.isSheetExists(SCENARIOS_SHEET)) {
            throw new FrameworkException("Required sheet not found: SCENARIOS.");
        }

        findRequiredColumnIndex(NO_COLUMN);
        findRequiredColumnIndex(RUN_COLUMN);
        findRequiredColumnIndex(ACTION_COLUMN);
        findRequiredColumnIndex(SCENARIOS_COLUMN);
    }

    private Scenario toScenario(int rowIndex) {
        String runValue = readCell(rowIndex, RUN_COLUMN);
        boolean run = parseRunValue(runValue, toExcelRowNumber(rowIndex));
        Scenario scenario = new Scenario(
                readCell(rowIndex, NO_COLUMN),
                run,
                readCell(rowIndex, ACTION_COLUMN),
                readCell(rowIndex, SCENARIOS_COLUMN),
                toExcelRowNumber(rowIndex)
        );

        if (scenario.getNo().isBlank()) {
            throw new FrameworkException("Scenario NO is required in sheet SCENARIOS row " + scenario.getExcelRowNumber() + ".");
        }
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
        throw new FrameworkException("Invalid RUN value '" + runValue + "' in sheet SCENARIOS row " + excelRowNumber + ". Allowed values: " + ALLOWED_RUN_VALUES + ".");
    }

    private void validateActiveScenario(Scenario scenario) {
        if (scenario.getAction().isBlank()) {
            throw new FrameworkException("ACTION is required for active scenario in sheet SCENARIOS row " + scenario.getExcelRowNumber() + ".");
        }
        if (scenario.getScenarioName().isBlank()) {
            throw new FrameworkException("SCENARIOS description is required for active scenario in sheet SCENARIOS row " + scenario.getExcelRowNumber() + ".");
        }
        if (!excelReader.isSheetExists(scenario.getAction())) {
            throw new FrameworkException("Scenario sheet not found: " + scenario.getAction() + ". Referenced by SCENARIOS row " + scenario.getExcelRowNumber() + ".");
        }
    }

    private void validateUniqueScenarioNumbers(List<Scenario> scenarios) {
        Map<String, Integer> rowByScenarioNumber = new LinkedHashMap<>();
        for (Scenario scenario : scenarios) {
            String scenarioNumber = scenario.getNo();
            if (scenarioNumber.isBlank()) {
                continue;
            }

            Integer existingRow = rowByScenarioNumber.putIfAbsent(scenarioNumber, scenario.getExcelRowNumber());
            if (existingRow != null) {
                throw new FrameworkException("Duplicate Scenario NO found in sheet SCENARIOS: " + scenarioNumber + ".");
            }
        }
    }

    private void validateUniqueActiveScenarioIdentities(List<Scenario> scenarios) {
        Map<String, Integer> rowByIdentity = new LinkedHashMap<>();
        for (Scenario scenario : scenarios) {
            if (!scenario.isRun()) {
                continue;
            }

            String identity = normalize(scenario.getNo()) + "::" + normalize(scenario.getAction());
            Integer existingRow = rowByIdentity.putIfAbsent(identity, scenario.getExcelRowNumber());
            if (existingRow != null) {
                throw new FrameworkException("Duplicate active scenario row found for NO = "
                        + scenario.getNo() + " and ACTION = " + scenario.getAction() + ".");
            }
        }
    }

    private boolean isScenarioRowBlank(int rowIndex) {
        return readCell(rowIndex, NO_COLUMN).isBlank()
                && readCell(rowIndex, RUN_COLUMN).isBlank()
                && readCell(rowIndex, ACTION_COLUMN).isBlank()
                && readCell(rowIndex, SCENARIOS_COLUMN).isBlank();
    }

    private String readCell(int rowIndex, String columnName) {
        int columnIndex = findRequiredColumnIndex(columnName);
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

    private int findRequiredColumnIndex(String columnName) {
        try {
            return excelReader.findColumnIndex(SCENARIOS_SHEET, columnName);
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("Header not found:")) {
                throw new FrameworkException("Header not found: " + columnName + " in sheet SCENARIOS.", exception);
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
