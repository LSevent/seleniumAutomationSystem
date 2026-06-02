package com.automation.excel;

import com.automation.exceptions.FrameworkException;
import com.automation.models.Scenario;
import com.automation.models.TestCaseBlock;
import com.automation.models.TestStep;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StepReader {

    private static final String TESTCASE_COLUMN = "Testcase";
    private static final String RUN_COLUMN = "Run";
    private static final String FUNCTION_COLUMN = "Function";
    private static final String OBJECT_COLUMN = "Object";
    private static final String VALUE_COLUMN = "Value";
    private static final String APPLICATION_COLUMN = "Application";
    private static final String DESCRIPTION_COLUMN = "Description";
    private static final String ALLOWED_RUN_VALUES = "Y, YES, TRUE, N, NO, FALSE, blank";

    private final ExcelReader excelReader;

    public StepReader(ExcelReader excelReader) {
        if (excelReader == null) {
            throw new IllegalArgumentException("ExcelReader must not be null.");
        }
        this.excelReader = excelReader;
    }

    public List<TestCaseBlock> getTestCases(Scenario scenario) {
        validateScenarioSheetExists(scenario);
        validateRequiredHeaders(scenario.getAction());

        List<TestCaseBlock> testCases = parseTestCases(scenario);
        validateDuplicateTestcaseNames(scenario.getAction(), testCases);
        validateActiveTestCasesHaveSteps(scenario.getAction(), testCases);
        return testCases;
    }

    public List<TestCaseBlock> getActiveTestCases(Scenario scenario) {
        return getTestCases(scenario).stream()
                .filter(TestCaseBlock::isRun)
                .toList();
    }

    public List<TestStep> getActiveSteps(Scenario scenario) {
        return getActiveTestCases(scenario).stream()
                .flatMap(testCaseBlock -> testCaseBlock.getSteps().stream())
                .toList();
    }

    public void validateScenarioSheet(Scenario scenario) {
        getTestCases(scenario);
    }

    private List<TestCaseBlock> parseTestCases(Scenario scenario) {
        String sheetName = scenario.getAction();
        List<TestCaseBlock> testCases = new ArrayList<>();
        TestCaseBlock currentTestCase = null;

        int lastRowNumber = excelReader.getLastRowNumber(sheetName);
        for (int rowIndex = 1; rowIndex <= lastRowNumber; rowIndex++) {
            RowData rowData = readRow(sheetName, rowIndex);
            if (rowData.isBlank()) {
                continue;
            }

            int excelRowNumber = toExcelRowNumber(rowIndex);
            if (!rowData.testcase().isBlank()) {
                currentTestCase = toTestCaseBlock(scenario, rowData, excelRowNumber);
                testCases.add(currentTestCase);
            } else if (!rowData.function().isBlank()) {
                if (currentTestCase == null) {
                    throw new FrameworkException("Step row found before any testcase parent row. Sheet: " + sheetName + ". Row: " + excelRowNumber + ".");
                }
                currentTestCase.addStep(toTestStep(scenario, currentTestCase, rowData, excelRowNumber));
            } else {
                throw new FrameworkException("Function is required for step row. Sheet: " + sheetName + ". Row: " + excelRowNumber + ".");
            }
        }
        return testCases;
    }

    private TestCaseBlock toTestCaseBlock(Scenario scenario, RowData rowData, int excelRowNumber) {
        String sheetName = scenario.getAction();
        boolean run = parseRunValue(rowData.run(), sheetName, excelRowNumber);

        if (!rowData.function().isBlank() || !rowData.object().isBlank() || !rowData.value().isBlank()) {
            throw new FrameworkException("Testcase parent row should not contain Function, Object, or Value. Sheet: " + sheetName + ". Row: " + excelRowNumber + ".");
        }
        if (run && rowData.application().isBlank()) {
            throw new FrameworkException("Application is required for active testcase '" + rowData.testcase() + "'. Sheet: " + sheetName + ". Row: " + excelRowNumber + ".");
        }

        return new TestCaseBlock(
                scenario.getNo(),
                scenario.getScenarioName(),
                scenario.getAction(),
                rowData.testcase(),
                run,
                rowData.application(),
                rowData.description(),
                excelRowNumber
        );
    }

    private TestStep toTestStep(Scenario scenario, TestCaseBlock testCaseBlock, RowData rowData, int excelRowNumber) {
        int stepOrder = testCaseBlock.getSteps().size() + 1;
        String application = rowData.application().isBlank() ? testCaseBlock.getApplication() : rowData.application();

        return new TestStep(
                scenario.getNo(),
                scenario.getScenarioName(),
                scenario.getAction(),
                testCaseBlock.getTestcaseName(),
                rowData.function(),
                rowData.object(),
                rowData.value(),
                application,
                rowData.description(),
                excelRowNumber,
                stepOrder
        );
    }

    private boolean parseRunValue(String runValue, String sheetName, int excelRowNumber) {
        String normalizedValue = normalize(runValue);
        if (normalizedValue.isEmpty() || "N".equals(normalizedValue) || "NO".equals(normalizedValue) || "FALSE".equals(normalizedValue)) {
            return false;
        }
        if ("Y".equals(normalizedValue) || "YES".equals(normalizedValue) || "TRUE".equals(normalizedValue)) {
            return true;
        }
        throw new FrameworkException("Invalid Run value '" + runValue + "' in sheet " + sheetName + " row " + excelRowNumber + ". Allowed values: " + ALLOWED_RUN_VALUES + ".");
    }

    private void validateScenarioSheetExists(Scenario scenario) {
        if (!excelReader.isSheetExists(scenario.getAction())) {
            throw new FrameworkException("Scenario sheet not found: " + scenario.getAction() + ". Referenced by SCENARIOS row " + scenario.getExcelRowNumber() + ".");
        }
    }

    private void validateRequiredHeaders(String sheetName) {
        findRequiredColumnIndex(sheetName, TESTCASE_COLUMN);
        findRequiredColumnIndex(sheetName, RUN_COLUMN);
        findRequiredColumnIndex(sheetName, FUNCTION_COLUMN);
        findRequiredColumnIndex(sheetName, OBJECT_COLUMN);
        findRequiredColumnIndex(sheetName, VALUE_COLUMN);
        findRequiredColumnIndex(sheetName, APPLICATION_COLUMN);
    }

    private void validateDuplicateTestcaseNames(String sheetName, List<TestCaseBlock> testCases) {
        Map<String, Integer> rowByTestcaseName = new LinkedHashMap<>();
        for (TestCaseBlock testCase : testCases) {
            String normalizedTestcaseName = normalize(testCase.getTestcaseName());
            Integer existingRow = rowByTestcaseName.putIfAbsent(normalizedTestcaseName, testCase.getExcelRowNumber());
            if (existingRow != null) {
                throw new FrameworkException("Duplicate testcase name '" + testCase.getTestcaseName() + "' found in sheet " + sheetName + ".");
            }
        }
    }

    private void validateActiveTestCasesHaveSteps(String sheetName, List<TestCaseBlock> testCases) {
        for (TestCaseBlock testCase : testCases) {
            if (testCase.isRun() && testCase.getSteps().isEmpty()) {
                throw new FrameworkException("Active testcase '" + testCase.getTestcaseName() + "' has no steps. Sheet: " + sheetName + ". Row: " + testCase.getExcelRowNumber() + ".");
            }
        }
    }

    private RowData readRow(String sheetName, int rowIndex) {
        return new RowData(
                readCell(sheetName, rowIndex, TESTCASE_COLUMN, true),
                readCell(sheetName, rowIndex, RUN_COLUMN, true),
                readCell(sheetName, rowIndex, FUNCTION_COLUMN, true),
                readCell(sheetName, rowIndex, OBJECT_COLUMN, true),
                readCell(sheetName, rowIndex, VALUE_COLUMN, true),
                readCell(sheetName, rowIndex, APPLICATION_COLUMN, true),
                readCell(sheetName, rowIndex, DESCRIPTION_COLUMN, false)
        );
    }

    private String readCell(String sheetName, int rowIndex, String columnName, boolean requiredHeader) {
        int columnIndex = requiredHeader ? findRequiredColumnIndex(sheetName, columnName) : findOptionalColumnIndex(sheetName, columnName);
        if (columnIndex < 0) {
            return "";
        }

        try {
            if (columnIndex >= excelReader.getColumnCount(sheetName, rowIndex)) {
                return "";
            }
            return excelReader.getCellValue(sheetName, rowIndex, columnIndex).trim();
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("Row not found: row " + rowIndex + " in sheet " + sheetName)) {
                return "";
            }
            throw exception;
        }
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

    private int findOptionalColumnIndex(String sheetName, String columnName) {
        try {
            return excelReader.findColumnIndex(sheetName, columnName);
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("Header not found:")) {
                return -1;
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

    private record RowData(
            String testcase,
            String run,
            String function,
            String object,
            String value,
            String application,
            String description
    ) {
        private boolean isBlank() {
            return testcase.isBlank()
                    && run.isBlank()
                    && function.isBlank()
                    && object.isBlank()
                    && value.isBlank()
                    && application.isBlank()
                    && description.isBlank();
        }
    }
}
