package com.automation.validation;

import com.automation.base.BaseFunction;
import com.automation.exceptions.ErrorContext;
import com.automation.exceptions.FrameworkException;
import com.automation.models.ConditionExpression;
import com.automation.models.FlowDirectiveType;
import com.automation.models.ResolvedScenarioContext;
import com.automation.models.ResolvedStepContext;
import com.automation.models.ResolvedTestcaseContext;
import com.automation.utils.XPathResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PreRunValidator {

    private static final String DATA_REFERENCE_FORMAT = "SHEET_NAME.COLUMN_NAME";
    private static final String SPECIFIC_FUNCTION_PACKAGE_PREFIX = "com.automation.functions.";
    private static final String SPECIFIC_FUNCTION_CLASS_SUFFIX = ".SpecificFunction";

    private final XPathResolver xpathResolver = new XPathResolver();

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

        String keyword = safe(step.getKeyword());
        if (keyword.isBlank()) {
            addStepError(errors, "Keyword is required for active step.", step);
        }
        if (isBlank(step.getApplication())) {
            addStepError(errors, "Application is required for active step.", step);
        }

        FlowDirectiveType flowDirective = step.getFlowDirective();
        if (flowDirective != null && flowDirective.isFlowDirective()) {
            if (flowDirective.isConditional()) {
                validateConditionalDirective(step, flowDirective, errors);
            } else if (flowDirective.isLoop()) {
                validateLoopDirective(step, flowDirective, errors);
            }
            return;
        }

        validateKeywordIsKnown(step, keyword, errors);

        String dataReferenceError = dataReferenceError(step.getRawValue());
        if (!dataReferenceError.isBlank()) {
            addStepError(errors, dataReferenceError, step);
        }

        validateDynamicXPath(step, errors);
        if (keyword.isBlank()) {
            return;
        }

        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        switch (normalizedKeyword) {
            case "screenshot" -> {
                // Screenshot intentionally has no Object/XPath/Value requirement.
            }
            case "openurl", "verifyurlcontains", "verifytitle", "verifytitlecontains" ->
                    requireValue(step, keyword, errors);
            case "click", "verifydisplayed", "clear", "gettext", "waitvisible", "waitclickable",
                    "scrolltoelement", "safeclick", "pressenter", "isdisplayed", "isnotdisplayed" ->
                    requireObjectAndXPath(step, keyword, errors);
            case "input", "verifytext", "verifytextcontains", "selectroombyname",
                    "verifybookingcreated", "verifyemployeevisible" -> {
                requireObjectAndXPath(step, keyword, errors);
                requireValue(step, keyword, errors);
            }
            default -> {
                // Phase 13B intentionally validates only the simple keyword rules above.
            }
        }
    }

    private void validateKeywordIsKnown(
            ResolvedStepContext step,
            String keyword,
            List<ValidationError> errors
    ) {
        if (keyword.isBlank() || isBlank(step.getApplication()) || isScreenshotKeyword(keyword)) {
            return;
        }
        if (hasNoArgumentKeywordMethod(BaseFunction.class, keyword)) {
            return;
        }

        Class<?> specificFunctionClass = loadSpecificFunctionClass(step.getApplication());
        if (specificFunctionClass != null && hasNoArgumentKeywordMethod(specificFunctionClass, keyword)) {
            return;
        }

        addStepError(
                errors,
                "Unknown keyword '" + keyword + "' for application '" + safe(step.getApplication()) + "'. "
                        + "Add a public no-argument method named '" + keyword + "' to "
                        + "SpecificFunction for application '" + safe(step.getApplication()) + "' or BaseFunction.",
                step
        );
    }

    private void validateConditionalDirective(
            ResolvedStepContext step,
            FlowDirectiveType flowDirective,
            List<ValidationError> errors
    ) {
        if ((flowDirective == FlowDirectiveType.IF_EQUALS || flowDirective == FlowDirectiveType.ELSE_IF_EQUALS)
                && isBlank(step.getRawValue())) {
            addStepError(errors, "Value condition is required for keyword '" + step.getKeyword() + "'.", step);
            return;
        }

        if (flowDirective == FlowDirectiveType.IF_EQUALS || flowDirective == FlowDirectiveType.ELSE_IF_EQUALS) {
            try {
                ConditionExpression condition = ConditionExpression.parse(step.getRawValue());
                String leftReferenceError = dataReferenceError(condition.getLeftOperand());
                if (!leftReferenceError.isBlank()) {
                    addStepError(errors, leftReferenceError, step);
                }
                String rightReferenceError = dataReferenceError(condition.getRightOperand());
                if (!rightReferenceError.isBlank()) {
                    addStepError(errors, rightReferenceError, step);
                }
            } catch (FrameworkException exception) {
                addStepError(errors, exception.getMessage(), step);
            }
        }
    }

    private void validateLoopDirective(
            ResolvedStepContext step,
            FlowDirectiveType flowDirective,
            List<ValidationError> errors
    ) {
        if (flowDirective == FlowDirectiveType.FOR_EACH_DATA_ROW && isBlank(step.getRawValue())) {
            addStepError(errors, "Data sheet name is required for keyword '" + step.getKeyword() + "'.", step);
        }
    }

    private void requireObjectAndXPath(
            ResolvedStepContext step,
            String keyword,
            List<ValidationError> errors
    ) {
        if (isBlank(step.getObjectName())) {
            addStepError(errors, "Object is required for keyword '" + keyword + "'.", step);
            return;
        }
        if (isBlank(step.getRawXPath())) {
            addStepError(errors, "Object was not resolved from OBJECT_REPOSITORY.", step);
        }
        if (isBlank(step.getResolvedXPath())) {
            addStepError(errors, "XPath is required for keyword '" + keyword + "'.", step);
        }
    }

    private void requireValue(
            ResolvedStepContext step,
            String keyword,
            List<ValidationError> errors
    ) {
        if (isBlank(step.getResolvedValue())) {
            addStepError(errors, "Value is required for keyword '" + keyword + "'.", step);
        }
    }

    private void validateDynamicXPath(ResolvedStepContext step, List<ValidationError> errors) {
        List<String> placeholders = xpathResolver.extractPlaceholders(step.getRawXPath());
        if (placeholders.size() > 1) {
            addStepError(errors, "Multiple XPath placeholders are not supported.", step);
            return;
        }
        if (placeholders.isEmpty()) {
            if (xpathResolver.hasPlaceholder(step.getResolvedXPath())) {
                addStepError(errors, "Dynamic XPath placeholder was not resolved.", step);
            }
            return;
        }

        String placeholderName = placeholders.get(0);
        String placeholder = "{" + placeholderName + "}";
        if (placeholderName.isBlank()) {
            addStepError(errors, "XPath placeholder cannot be blank.", step);
            return;
        }

        String dataColumn = dataReferenceColumn(step.getRawValue());
        if (!dataColumn.isBlank() && !placeholderName.equalsIgnoreCase(dataColumn)) {
            addStepError(errors, "XPath placeholder " + placeholder + " does not match data column '" + dataColumn + "'.", step);
        }
        if (isBlank(step.getResolvedValue())
                || isBlank(step.getResolvedXPath())
                || xpathResolver.hasPlaceholder(step.getResolvedXPath())) {
            addStepError(errors, "XPath placeholder " + placeholder + " was not resolved.", step);
        }
    }

    private String dataReferenceError(String rawValue) {
        String value = safe(rawValue);
        if (value.isBlank()) {
            return "";
        }
        if (value.contains("[") || value.contains("]")) {
            return invalidReferenceMessage(value);
        }

        long dotCount = value.chars().filter(character -> character == '.').count();
        if (dotCount == 0) {
            return "";
        }
        if (dotCount != 1) {
            return invalidReferenceMessage(value);
        }

        String[] parts = value.split("\\.", -1);
        return parts[0].trim().isBlank() || parts[1].trim().isBlank()
                ? invalidReferenceMessage(value)
                : "";
    }

    private String dataReferenceColumn(String rawValue) {
        String value = safe(rawValue);
        if (!dataReferenceError(value).isBlank()) {
            return "";
        }
        String[] parts = value.split("\\.", -1);
        return parts.length == 2 ? parts[1].trim() : "";
    }

    private String invalidReferenceMessage(String rawValue) {
        return "Invalid data reference format: " + rawValue + ". Expected format: " + DATA_REFERENCE_FORMAT + ".";
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

    private boolean isScreenshotKeyword(String keyword) {
        return "screenshot".equalsIgnoreCase(safe(keyword));
    }

    private Class<?> loadSpecificFunctionClass(String application) {
        try {
            return Class.forName(
                    SPECIFIC_FUNCTION_PACKAGE_PREFIX
                            + safe(application).toUpperCase(Locale.ROOT)
                            + SPECIFIC_FUNCTION_CLASS_SUFFIX,
                    false,
                    Thread.currentThread().getContextClassLoader()
            );
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    private boolean hasNoArgumentKeywordMethod(Class<?> type, String keyword) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(keyword)
                    && Modifier.isPublic(method.getModifiers())
                    && method.getParameterCount() == 0) {
                return true;
            }
        }
        return false;
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
