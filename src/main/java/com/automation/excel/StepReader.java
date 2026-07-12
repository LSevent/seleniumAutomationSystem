package com.automation.excel;

import com.automation.exceptions.FrameworkException;
import com.automation.models.FlowDirectiveType;
import com.automation.models.Scenario;
import com.automation.models.TestCaseBlock;
import com.automation.models.TestStep;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StepReader {

    private static final String TESTCASE_COLUMN = "Testcase";
    private static final String RUN_COLUMN = "Run";
    private static final String KEYWORD_COLUMN = "Keyword";
    private static final String LEGACY_FUNCTION_COLUMN = "Function";
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
        validateFlowBlocks(scenario.getAction(), testCases);
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
            } else if (!rowData.keyword().isBlank()) {
                if (currentTestCase == null) {
                    throw new FrameworkException("Step row found before any testcase parent row. Sheet: " + sheetName + ". Row: " + excelRowNumber + ".");
                }
                currentTestCase.addStep(toTestStep(scenario, currentTestCase, rowData, excelRowNumber));
            } else {
                throw new FrameworkException("Keyword is required for step row. Sheet: " + sheetName + ". Row: " + excelRowNumber + ".");
            }
        }
        return testCases;
    }

    private TestCaseBlock toTestCaseBlock(Scenario scenario, RowData rowData, int excelRowNumber) {
        String sheetName = scenario.getAction();
        boolean run = parseRunValue(rowData.run(), sheetName, excelRowNumber);

        if (!rowData.keyword().isBlank() || !rowData.object().isBlank() || !rowData.value().isBlank()) {
            throw new FrameworkException("Testcase parent row should not contain Keyword, Object, or Value. Sheet: " + sheetName + ". Row: " + excelRowNumber + ".");
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
        boolean run = parseStepRunValue(rowData.run(), scenario.getAction(), excelRowNumber);
        FlowDirectiveType flowDirective = FlowDirectiveType.fromKeyword(rowData.keyword());
        if (!run && flowDirective.isFlowDirective()) {
            throw new FrameworkException(
                    "Run=No is not supported for flow directive '" + rowData.keyword()
                            + "' in sheet " + scenario.getAction() + " row " + excelRowNumber
                            + ". Keep flow directive rows active and use conditions to control their blocks."
            );
        }

        return TestStep.builder()
                .scenarioNo(scenario.getNo())
                .scenarioName(scenario.getScenarioName())
                .scenarioAction(scenario.getAction())
                .testcaseName(testCaseBlock.getTestcaseName())
                .keyword(rowData.keyword())
                .object(rowData.object())
                .value(rowData.value())
                .application(application)
                .description(rowData.description())
                .run(run)
                .excelRowNumber(excelRowNumber)
                .stepOrder(stepOrder)
                .flowDirective(flowDirective)
                .build();
    }

    private boolean parseStepRunValue(String runValue, String sheetName, int excelRowNumber) {
        String normalizedValue = normalize(runValue);
        if (normalizedValue.isEmpty() || "Y".equals(normalizedValue)
                || "YES".equals(normalizedValue) || "TRUE".equals(normalizedValue)) {
            return true;
        }
        if ("N".equals(normalizedValue) || "NO".equals(normalizedValue) || "FALSE".equals(normalizedValue)) {
            return false;
        }
        throw new FrameworkException(
                "Invalid Run value '" + runValue + "' for step in sheet " + sheetName + " row "
                        + excelRowNumber + ". Allowed values: " + ALLOWED_RUN_VALUES + "."
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
        findKeywordColumnName(sheetName);
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

    private void validateFlowBlocks(String sheetName, List<TestCaseBlock> testCases) {
        for (TestCaseBlock testCase : testCases) {
            if (testCase.isRun()) {
                validateFlowBlocks(sheetName, testCase);
            }
        }
    }

    private void validateFlowBlocks(String sheetName, TestCaseBlock testCase) {
        Deque<FlowBlock> openBlocks = new ArrayDeque<>();
        for (TestStep step : testCase.getSteps()) {
            FlowDirectiveType directive = step.getFlowDirective();
            if (directive == null || !directive.isFlowDirective()) {
                continue;
            }

            switch (directive) {
                case IF_EQUALS, IF_DISPLAYED -> openBlocks.push(FlowBlock.conditional(step.getExcelRowNumber()));
                case ELSE_IF_EQUALS, ELSE_IF_DISPLAYED -> validateElseIf(sheetName, testCase, step, openBlocks);
                case ELSE -> validateElse(sheetName, testCase, step, openBlocks);
                case END_IF -> validateEndIf(sheetName, testCase, step, openBlocks);
                case FOR_EACH_DATA_ROW -> openBlocks.push(FlowBlock.loop(step.getExcelRowNumber()));
                case END_FOR_EACH_DATA_ROW -> validateEndForEachDataRow(sheetName, testCase, step, openBlocks);
                default -> {
                    // Not a flow directive.
                }
            }
        }

        if (!openBlocks.isEmpty()) {
            FlowBlock block = openBlocks.peek();
            throw flowBlockError(
                    block.missingEndMessage(),
                    sheetName,
                    testCase,
                    block.startRow()
            );
        }
    }

    private void validateElseIf(
            String sheetName,
            TestCaseBlock testCase,
            TestStep step,
            Deque<FlowBlock> openBlocks
    ) {
        FlowBlock currentBlock = requireOpenConditionalBlock(step.getKeyword(), sheetName, testCase, step, openBlocks);
        if (currentBlock.hasElse()) {
            throw flowBlockError(
                    "Conditional directive '" + step.getKeyword() + "' cannot appear after 'else' in the same conditional block.",
                    sheetName,
                    testCase,
                    step.getExcelRowNumber()
            );
        }
    }

    private void validateElse(
            String sheetName,
            TestCaseBlock testCase,
            TestStep step,
            Deque<FlowBlock> openBlocks
    ) {
        FlowBlock currentBlock = requireOpenConditionalBlock("else", sheetName, testCase, step, openBlocks);
        if (currentBlock.hasElse()) {
            throw flowBlockError(
                    "Conditional directive 'else' appears more than once in the same conditional block.",
                    sheetName,
                    testCase,
                    step.getExcelRowNumber()
            );
        }
        currentBlock.markElseSeen();
    }

    private void validateEndIf(
            String sheetName,
            TestCaseBlock testCase,
            TestStep step,
            Deque<FlowBlock> openBlocks
    ) {
        requireOpenConditionalBlock("endIf", sheetName, testCase, step, openBlocks);
        openBlocks.pop();
    }

    private void validateEndForEachDataRow(
            String sheetName,
            TestCaseBlock testCase,
            TestStep step,
            Deque<FlowBlock> openBlocks
    ) {
        requireOpenLoopBlock("endForEachDataRow", sheetName, testCase, step, openBlocks);
        openBlocks.pop();
    }

    private FlowBlock requireOpenConditionalBlock(
            String directive,
            String sheetName,
            TestCaseBlock testCase,
            TestStep step,
            Deque<FlowBlock> openBlocks
    ) {
        if (openBlocks.isEmpty()) {
            throw flowBlockError(
                    "Conditional directive '" + directive + "' found without an open 'ifEquals' or 'ifDisplayed'.",
                    sheetName,
                    testCase,
                    step.getExcelRowNumber()
            );
        }
        FlowBlock block = openBlocks.peek();
        if (!block.isConditional()) {
            throw flowBlockError(
                    "Conditional directive '" + directive + "' cannot close or branch a 'forEachDataRow' block before 'endForEachDataRow'.",
                    sheetName,
                    testCase,
                    step.getExcelRowNumber()
            );
        }
        return block;
    }

    private FlowBlock requireOpenLoopBlock(
            String directive,
            String sheetName,
            TestCaseBlock testCase,
            TestStep step,
            Deque<FlowBlock> openBlocks
    ) {
        if (openBlocks.isEmpty()) {
            throw flowBlockError(
                    "Loop directive '" + directive + "' found without an open 'forEachDataRow'.",
                    sheetName,
                    testCase,
                    step.getExcelRowNumber()
            );
        }
        FlowBlock block = openBlocks.peek();
        if (!block.isLoop()) {
            throw flowBlockError(
                    "Loop directive '" + directive + "' cannot close an 'ifEquals' block before 'endIf'.",
                    sheetName,
                    testCase,
                    step.getExcelRowNumber()
            );
        }
        return block;
    }

    private FrameworkException flowBlockError(
            String message,
            String sheetName,
            TestCaseBlock testCase,
            int excelRowNumber
    ) {
        return new FrameworkException(
                message
                        + " Sheet: " + sheetName
                        + ". Testcase: " + testCase.getTestcaseName()
                        + ". Row: " + excelRowNumber
                        + "."
        );
    }

    private RowData readRow(String sheetName, int rowIndex) {
        return new RowData(
                readCell(sheetName, rowIndex, TESTCASE_COLUMN, true),
                readCell(sheetName, rowIndex, RUN_COLUMN, true),
                readCell(sheetName, rowIndex, findKeywordColumnName(sheetName), true),
                readCell(sheetName, rowIndex, OBJECT_COLUMN, true),
                readValueCell(sheetName, rowIndex),
                readCell(sheetName, rowIndex, APPLICATION_COLUMN, true),
                readCell(sheetName, rowIndex, DESCRIPTION_COLUMN, false)
        );
    }

    private String readValueCell(String sheetName, int rowIndex) {
        int columnIndex = findRequiredColumnIndex(sheetName, VALUE_COLUMN);
        try {
            if (columnIndex >= excelReader.getColumnCount(sheetName, rowIndex)) {
                return "";
            }
            return excelReader.getCellValuePreservingFormulaHeaderReference(sheetName, rowIndex, columnIndex).trim();
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("Row not found: row " + rowIndex + " in sheet " + sheetName)) {
                return "";
            }
            throw exception;
        }
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

    private String findKeywordColumnName(String sheetName) {
        if (findOptionalColumnIndex(sheetName, KEYWORD_COLUMN) >= 0) {
            return KEYWORD_COLUMN;
        }
        if (findOptionalColumnIndex(sheetName, LEGACY_FUNCTION_COLUMN) >= 0) {
            return LEGACY_FUNCTION_COLUMN;
        }
        throw new FrameworkException("Missing required column 'Keyword' in sheet '" + sheetName
                + "'. Legacy column 'Function' is also supported, but neither was found.");
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
            String keyword,
            String object,
            String value,
            String application,
            String description
    ) {
        private boolean isBlank() {
            return testcase.isBlank()
                    && run.isBlank()
                    && keyword.isBlank()
                    && object.isBlank()
                    && value.isBlank()
                    && application.isBlank()
                    && description.isBlank();
        }
    }

    private static final class FlowBlock {

        private final FlowBlockKind kind;
        private final int startRow;
        private boolean hasElse;

        private FlowBlock(FlowBlockKind kind, int startRow) {
            this.kind = kind;
            this.startRow = startRow;
        }

        private static FlowBlock conditional(int startRow) {
            return new FlowBlock(FlowBlockKind.CONDITIONAL, startRow);
        }

        private static FlowBlock loop(int startRow) {
            return new FlowBlock(FlowBlockKind.LOOP, startRow);
        }

        private int startRow() {
            return startRow;
        }

        private boolean isConditional() {
            return kind == FlowBlockKind.CONDITIONAL;
        }

        private boolean isLoop() {
            return kind == FlowBlockKind.LOOP;
        }

        private boolean hasElse() {
            return hasElse;
        }

        private void markElseSeen() {
            hasElse = true;
        }

        private String missingEndMessage() {
            if (isLoop()) {
                return "Loop block starting with 'forEachDataRow' is missing 'endForEachDataRow'.";
            }
            return "Conditional block starting with 'ifEquals' or 'ifDisplayed' is missing 'endIf'.";
        }
    }

    private enum FlowBlockKind {
        CONDITIONAL,
        LOOP
    }
}
