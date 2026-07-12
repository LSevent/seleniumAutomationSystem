package com.automation.validation;

import com.automation.exceptions.ErrorContext;
import com.automation.exceptions.FrameworkException;
import com.automation.models.ConditionExpression;
import com.automation.models.FlowDirectiveType;
import com.automation.models.ResolvedScenarioContext;
import com.automation.models.ResolvedStepContext;
import com.automation.models.ResolvedTestcaseContext;

import java.util.ArrayList;
import java.util.List;

public class PreRunValidator {

    public void validate(List<ResolvedScenarioContext> executionPlan) {
        if (executionPlan == null) {
            throw new IllegalArgumentException("Execution plan must not be null.");
        }

        List<ValidationError> errors = new ArrayList<>();
        for (ResolvedScenarioContext scenario : executionPlan) {
            validateScenario(scenario, errors);
        }

        if (!errors.isEmpty()) {
            throw new FrameworkException(formatErrors(errors));
        }
    }

    private void validateScenario(ResolvedScenarioContext scenario, List<ValidationError> errors) {
        if (scenario == null) {
            errors.add(new ValidationError("Resolved scenario context is not available.", "", "", "", 0, "", "", ""));
            return;
        }
        if (scenario.getTestcases().isEmpty()) {
            errors.add(new ValidationError(
                    "Active scenario has no active testcase.",
                    scenario.getScenarioNo(),
                    scenario.getScenarioAction(),
                    scenario.getScenarioAction(),
                    0,
                    "",
                    "",
                    ""
            ));
            return;
        }

        for (ResolvedTestcaseContext testcase : scenario.getTestcases()) {
            validateTestcase(scenario, testcase, errors);
        }
    }

    private void validateTestcase(
            ResolvedScenarioContext scenario,
            ResolvedTestcaseContext testcase,
            List<ValidationError> errors
    ) {
        if (testcase == null) {
            errors.add(new ValidationError(
                    "Resolved testcase context is not available.",
                    scenario.getScenarioNo(),
                    scenario.getScenarioAction(),
                    scenario.getScenarioAction(),
                    0,
                    "",
                    "",
                    ""
            ));
            return;
        }

        if (testcase.getSteps().isEmpty()) {
            errors.add(new ValidationError(
                    "Active testcase has no steps.",
                    scenario.getScenarioNo(),
                    scenario.getScenarioAction(),
                    scenario.getScenarioAction(),
                    testcase.getTestcaseName(),
                    testcase.getParentExcelRow(),
                    "",
                    "",
                    testcase.getApplication()
            ));
            return;
        }

        for (ResolvedStepContext step : testcase.getSteps()) {
            validateStep(step, errors);
        }
    }

    private void validateStep(ResolvedStepContext step, List<ValidationError> errors) {
        if (step == null) {
            errors.add(new ValidationError("Resolved step context is not available.", "", "", "", 0, "", "", ""));
            return;
        }

        if (!step.isRun()) {
            return;
        }

        String keyword = safe(step.getKeyword());
        if (keyword.isBlank()) {
            addStepError(errors, "Keyword is required for active step.", step);
        }
        if (isBlank(step.getApplication())) {
            addStepError(errors, "Application is required for active step.", step);
        }

        FlowDirectiveType flowDirective = step.getFlowDirective();
        if (flowDirective == null || !flowDirective.isFlowDirective()) {
            return;
        }

        if (flowDirective.isEqualityCondition()) {
            validateEqualityCondition(step, errors);
        } else if (flowDirective == FlowDirectiveType.FOR_EACH_DATA_ROW && isBlank(step.getRawValue())) {
            addStepError(errors, "Data sheet name is required for keyword '" + step.getKeyword() + "'.", step);
        }
    }

    private void validateEqualityCondition(ResolvedStepContext step, List<ValidationError> errors) {
        if (isBlank(step.getRawValue())) {
            addStepError(errors, "Value condition is required for keyword '" + step.getKeyword() + "'.", step);
            return;
        }

        if (!ConditionExpression.hasComparisonOperator(step.getRawValue())) {
            if (isBlank(step.getDescription())) {
                addStepError(
                        errors,
                        "Expected value is required in Description for keyword '" + step.getKeyword()
                                + "' when Value contains only the actual value. "
                                + "Use Value as ACTUAL = EXPECTED or Value as ACTUAL and Description as EXPECTED.",
                        step
                );
            }
            return;
        }

        try {
            ConditionExpression.parse(step.getRawValue());
        } catch (FrameworkException exception) {
            addStepError(errors, exception.getMessage(), step);
        }
    }

    private void addStepError(List<ValidationError> errors, String message, ResolvedStepContext step) {
        errors.add(new ValidationError(
                message,
                step.getScenarioNo(),
                step.getScenarioAction(),
                step.getSheetName(),
                step.getTestcaseName(),
                step.getExcelRow(),
                step.getKeyword(),
                step.getObjectName(),
                step.getApplication()
        ));
    }

    private String formatErrors(List<ValidationError> errors) {
        String lineSeparator = System.lineSeparator();
        StringBuilder message = new StringBuilder("Pre-run validation failed with ")
                .append(errors.size())
                .append(" error(s).")
                .append(lineSeparator);

        for (int index = 0; index < errors.size(); index++) {
            ValidationError error = errors.get(index);
            message.append(lineSeparator)
                    .append(index + 1)
                    .append(". ")
                    .append(error.message());

            String context = error.context();
            if (!context.isBlank()) {
                message.append(lineSeparator)
                        .append("   ")
                        .append(context.replace(lineSeparator, lineSeparator + "   "));
            }
        }
        return message.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record ValidationError(
            String message,
            String scenarioNo,
            String scenarioAction,
            String sheetName,
            String testcaseName,
            int row,
            String keyword,
            String objectName,
            String application
    ) {
        private ValidationError(
                String message,
                String scenarioNo,
                String scenarioAction,
                String sheetName,
                int row,
                String keyword,
                String objectName,
                String application
        ) {
            this(message, scenarioNo, scenarioAction, sheetName, "", row, keyword, objectName, application);
        }

        private String context() {
            return new ErrorContext()
                    .scenarioNo(scenarioNo)
                    .scenarioAction(scenarioAction)
                    .sheet(sheetName)
                    .testcase(testcaseName)
                    .row(row)
                    .keyword(keyword)
                    .object(objectName)
                    .application(application)
                    .render();
        }
    }
}
