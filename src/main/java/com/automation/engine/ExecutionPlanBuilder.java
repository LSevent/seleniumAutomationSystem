package com.automation.engine;

import com.automation.excel.DataReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.exceptions.ErrorContext;
import com.automation.exceptions.FrameworkException;
import com.automation.models.ConditionExpression;
import com.automation.models.DataReference;
import com.automation.models.FlowDirectiveType;
import com.automation.models.ResolvedObject;
import com.automation.models.ResolvedScenarioContext;
import com.automation.models.ResolvedStepContext;
import com.automation.models.ResolvedTestcaseContext;
import com.automation.models.Scenario;
import com.automation.models.TestCaseBlock;
import com.automation.models.TestStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExecutionPlanBuilder {

    private final ScenarioReader scenarioReader;
    private final StepReader stepReader;
    private final DataReader dataReader;
    private final ObjectRepositoryReader objectRepositoryReader;

    public ExecutionPlanBuilder(
            ScenarioReader scenarioReader,
            StepReader stepReader,
            DataReader dataReader,
            ObjectRepositoryReader objectRepositoryReader
    ) {
        if (scenarioReader == null) {
            throw new IllegalArgumentException("ScenarioReader must not be null.");
        }
        if (stepReader == null) {
            throw new IllegalArgumentException("StepReader must not be null.");
        }
        if (dataReader == null) {
            throw new IllegalArgumentException("DataReader must not be null.");
        }
        if (objectRepositoryReader == null) {
            throw new IllegalArgumentException("ObjectRepositoryReader must not be null.");
        }
        this.scenarioReader = scenarioReader;
        this.stepReader = stepReader;
        this.dataReader = dataReader;
        this.objectRepositoryReader = objectRepositoryReader;
    }

    public List<ResolvedScenarioContext> build() {
        List<ResolvedScenarioContext> executionPlan = new ArrayList<>();
        for (Scenario scenario : scenarioReader.getActiveScenarios()) {
            executionPlan.add(resolveScenario(scenario));
        }
        return List.copyOf(executionPlan);
    }

    private ResolvedScenarioContext resolveScenario(Scenario scenario) {
        List<ResolvedTestcaseContext> resolvedTestcases = stepReader.getActiveTestCases(scenario).stream()
                .map(testcase -> resolveTestcase(scenario, testcase))
                .toList();

        return new ResolvedScenarioContext(
                scenario.getNo(),
                scenario.getAction(),
                scenario.getScenarioName(),
                resolvedTestcases
        );
    }

    private ResolvedTestcaseContext resolveTestcase(Scenario scenario, TestCaseBlock testcase) {
        List<ResolvedStepContext> resolvedSteps = resolveSteps(scenario, testcase, testcase.getSteps(), null);

        return new ResolvedTestcaseContext(
                testcase.getTestcaseName(),
                testcase.getApplication(),
                testcase.getExcelRowNumber(),
                resolvedSteps
        );
    }

    private List<ResolvedStepContext> resolveSteps(
            Scenario scenario,
            TestCaseBlock testcase,
            List<TestStep> steps,
            LoopDataContext loopDataContext
    ) {
        List<ResolvedStepContext> resolvedSteps = new ArrayList<>();
        for (int index = 0; index < steps.size(); index++) {
            TestStep step = steps.get(index);
            if (step.getFlowDirective() == FlowDirectiveType.FOR_EACH_DATA_ROW) {
                int endIndex = findMatchingEndForEach(steps, index);
                TestStep endStep = steps.get(endIndex);
                List<TestStep> loopBody = steps.subList(index + 1, endIndex);
                resolvedSteps.addAll(resolveLoopSteps(scenario, testcase, step, endStep, loopBody, loopDataContext));
                index = endIndex;
            } else {
                resolvedSteps.add(resolveStep(scenario, testcase, step, loopDataContext, null));
            }
        }
        return List.copyOf(resolvedSteps);
    }

    private List<ResolvedStepContext> resolveLoopSteps(
            Scenario scenario,
            TestCaseBlock testcase,
            TestStep loopStartStep,
            TestStep loopEndStep,
            List<TestStep> loopBody,
            LoopDataContext parentLoopContext
    ) {
        String dataSheetName = loopDataSheetName(loopStartStep);
        List<Map<String, String>> dataRows;
        try {
            dataRows = dataReader.getDataRows(dataSheetName, scenario.getNo());
        } catch (RuntimeException exception) {
            throw resolutionFailure("Loop data rows could not be resolved while building the execution plan.", scenario, testcase, loopStartStep, exception);
        }

        List<ResolvedStepContext> resolvedSteps = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < dataRows.size(); rowIndex++) {
            int iterationNumber = rowIndex + 1;
            String iterationLabel = dataSheetName + " row " + iterationNumber + " of " + dataRows.size();
            LoopDataContext loopDataContext = new LoopDataContext(
                    dataSheetName,
                    dataRows.get(rowIndex),
                    iterationNumber,
                    dataRows.size(),
                    parentLoopContext
            );

            resolvedSteps.add(resolveStep(scenario, testcase, loopStartStep, parentLoopContext, iterationLabel));
            resolvedSteps.addAll(resolveSteps(scenario, testcase, loopBody, loopDataContext));
            resolvedSteps.add(resolveStep(scenario, testcase, loopEndStep, parentLoopContext, iterationLabel));
        }
        return resolvedSteps;
    }

    private ResolvedStepContext resolveStep(
            Scenario scenario,
            TestCaseBlock testcase,
            TestStep step,
            LoopDataContext loopDataContext,
            String flowResolvedValueOverride
    ) {
        String rawValue = safe(step.getValue());
        String resolvedValue = rawValue;
        if (step.isRun()) {
            try {
                resolvedValue = flowResolvedValueOverride != null
                        ? flowResolvedValueOverride
                        : step.isFlowDirective()
                        ? resolveFlowDirectiveValue(step, scenario, loopDataContext)
                        : resolveValue(rawValue, scenario, loopDataContext);
            } catch (RuntimeException exception) {
                throw resolutionFailure("Value could not be resolved while building the execution plan.", scenario, testcase, step, exception);
            }
        }

        String rawXPath = "";
        String resolvedXPath = "";
        if (step.isRun() && !step.isFlowDirective() && !isBlank(step.getObject())) {
            try {
                ResolvedObject resolvedObject = objectRepositoryReader.resolveObject(step, rawValue, resolvedValue);
                if (resolvedObject != null) {
                    rawXPath = safe(resolvedObject.getRawXPath());
                    resolvedXPath = safe(resolvedObject.getResolvedXPath());
                    resolvedValue = safe(resolvedObject.getResolvedValue());
                }
            } catch (RuntimeException exception) {
                throw resolutionFailure("Object or XPath could not be resolved while building the execution plan.", scenario, testcase, step, exception);
            }
        }

        return ResolvedStepContext.builder()
                .scenarioNo(scenario.getNo())
                .scenarioAction(scenario.getAction())
                .scenarioName(scenario.getScenarioName())
                .sheetName(scenario.getAction())
                .testcaseName(testcase.getTestcaseName())
                .testcaseParentRow(testcase.getExcelRowNumber())
                .excelRow(step.getExcelRowNumber())
                .stepNumber(step.getStepOrder())
                .keyword(safe(step.getKeyword()))
                .objectName(safe(step.getObject()))
                .application(safe(step.getApplication()))
                .description(safe(step.getDescription()))
                .rawValue(rawValue)
                .resolvedValue(resolvedValue)
                .rawXPath(rawXPath)
                .resolvedXPath(resolvedXPath)
                .executedBy("")
                .run(step.isRun())
                .flowDirective(step.getFlowDirective())
                .build();
    }

    private String resolveFlowDirectiveValue(TestStep step, Scenario scenario, LoopDataContext loopDataContext) {
        if (step.getFlowDirective() == null
                || !(step.getFlowDirective().startsConditionalBlock()
                || step.getFlowDirective() == FlowDirectiveType.ELSE_IF_EQUALS)) {
            return safe(step.getValue());
        }

        ConditionExpression condition = ConditionExpression.parse(step.getValue());
        String leftValue = resolveValue(condition.getLeftOperand(), scenario, loopDataContext);
        String rightValue = resolveValue(condition.getRightOperand(), scenario, loopDataContext);
        return safe(leftValue) + " = " + safe(rightValue);
    }

    private String resolveValue(String rawValue, Scenario scenario, LoopDataContext loopDataContext) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }
        if (!dataReader.isDataReference(rawValue)) {
            return rawValue;
        }

        DataReference dataReference = dataReader.parseReference(rawValue);
        LoopDataContext matchingLoopContext = loopDataContext == null
                ? null
                : loopDataContext.find(dataReference.getSheetName());
        if (matchingLoopContext != null) {
            return dataReader.resolveValue(
                    rawValue,
                    scenario,
                    matchingLoopContext.sheetName(),
                    matchingLoopContext.dataRow()
            );
        }
        return dataReader.resolveValue(rawValue, scenario);
    }

    private int findMatchingEndForEach(List<TestStep> steps, int startIndex) {
        int depth = 0;
        for (int index = startIndex; index < steps.size(); index++) {
            FlowDirectiveType directive = steps.get(index).getFlowDirective();
            if (directive == FlowDirectiveType.FOR_EACH_DATA_ROW) {
                depth++;
            } else if (directive == FlowDirectiveType.END_FOR_EACH_DATA_ROW) {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        throw new FrameworkException("Loop block starting with 'forEachDataRow' is missing 'endForEachDataRow'.");
    }

    private String loopDataSheetName(TestStep step) {
        String rawSheetName = safe(step.getValue());
        if (rawSheetName.startsWith("#")) {
            rawSheetName = rawSheetName.substring(1).trim();
        }
        if (rawSheetName.isBlank()) {
            throw new FrameworkException("Data sheet name is required for keyword 'forEachDataRow'.");
        }
        return rawSheetName;
    }

    private FrameworkException resolutionFailure(
            String message,
            Scenario scenario,
            TestCaseBlock testcase,
            TestStep step,
            RuntimeException cause
    ) {
        String context = new ErrorContext()
                .scenarioNo(scenario.getNo())
                .scenarioAction(scenario.getAction())
                .sheet(scenario.getAction())
                .testcase(testcase.getTestcaseName())
                .row(step.getExcelRowNumber())
                .keyword(step.getKeyword())
                .object(step.getObject())
                .application(step.getApplication())
                .render();

        return new FrameworkException(
                message + System.lineSeparator()
                        + context + System.lineSeparator()
                        + "Cause: " + safe(cause.getMessage()),
                cause
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record LoopDataContext(
            String sheetName,
            Map<String, String> dataRow,
            int iterationNumber,
            int totalIterations,
            LoopDataContext parent
    ) {
        private LoopDataContext find(String candidateSheetName) {
            if (candidateSheetName != null && sheetName.trim().equalsIgnoreCase(candidateSheetName.trim())) {
                return this;
            }
            return parent == null ? null : parent.find(candidateSheetName);
        }
    }
}
