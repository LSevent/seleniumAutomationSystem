package com.automation.engine;

import com.automation.excel.DataReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.exceptions.ErrorContext;
import com.automation.exceptions.FrameworkException;
import com.automation.models.ResolvedObject;
import com.automation.models.ResolvedScenarioContext;
import com.automation.models.ResolvedStepContext;
import com.automation.models.ResolvedTestcaseContext;
import com.automation.models.Scenario;
import com.automation.models.TestCaseBlock;
import com.automation.models.TestStep;

import java.util.ArrayList;
import java.util.List;

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
        List<ResolvedStepContext> resolvedSteps = testcase.getSteps().stream()
                .map(step -> resolveStep(scenario, testcase, step))
                .toList();

        return new ResolvedTestcaseContext(
                testcase.getTestcaseName(),
                testcase.getApplication(),
                testcase.getExcelRowNumber(),
                resolvedSteps
        );
    }

    private ResolvedStepContext resolveStep(Scenario scenario, TestCaseBlock testcase, TestStep step) {
        String rawValue = safe(step.getValue());
        String resolvedValue;
        try {
            resolvedValue = dataReader.resolveValue(rawValue, scenario);
        } catch (RuntimeException exception) {
            throw resolutionFailure("Value could not be resolved while building the execution plan.", scenario, testcase, step, exception);
        }

        String rawXPath = "";
        String resolvedXPath = "";
        if (!isBlank(step.getObject())) {
            try {
                ResolvedObject resolvedObject = objectRepositoryReader.resolveObject(step, scenario);
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
                .build();
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
}
