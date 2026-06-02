package com.automation.models;

public class ExecutionResult {

    public static final String STATUS_PASS = "PASS";
    public static final String STATUS_FAIL = "FAIL";
    public static final String STATUS_SKIP = "SKIP";

    private final String scenarioNo;
    private final String scenarioName;
    private final String scenarioAction;
    private final String testcaseName;
    private final String description;
    private final String functionName;
    private final String objectName;
    private final String application;
    private final String rawValue;
    private final String resolvedValue;
    private final String rawXpath;
    private final String resolvedXpath;
    private final String executedByClass;
    private final String executionSource;
    private final boolean success;
    private final String status;
    private final String evidence;
    private final String message;
    private final int excelRowNumber;
    private final int stepOrder;

    public ExecutionResult(
            String scenarioNo,
            String scenarioName,
            String scenarioAction,
            String testcaseName,
            String description,
            String functionName,
            String objectName,
            String application,
            String rawValue,
            String resolvedValue,
            String rawXpath,
            String resolvedXpath,
            String executedByClass,
            String executionSource,
            boolean success,
            String status,
            String evidence,
            String message,
            int excelRowNumber,
            int stepOrder
    ) {
        this.scenarioNo = scenarioNo;
        this.scenarioName = scenarioName;
        this.scenarioAction = scenarioAction;
        this.testcaseName = testcaseName;
        this.description = description;
        this.functionName = functionName;
        this.objectName = objectName;
        this.application = application;
        this.rawValue = rawValue;
        this.resolvedValue = resolvedValue;
        this.rawXpath = rawXpath;
        this.resolvedXpath = resolvedXpath;
        this.executedByClass = executedByClass;
        this.executionSource = executionSource;
        this.success = success;
        this.status = status;
        this.evidence = evidence;
        this.message = message;
        this.excelRowNumber = excelRowNumber;
        this.stepOrder = stepOrder;
    }

    public static ExecutionResult success(
            Scenario scenario,
            TestStep step,
            String resolvedValue,
            String rawXpath,
            String resolvedXpath,
            String executedByClass,
            String executionSource,
            String message
    ) {
        return fromStep(scenario, step, resolvedValue, rawXpath, resolvedXpath, executedByClass, executionSource, true, STATUS_PASS, "", message);
    }

    public static ExecutionResult success(
            Scenario scenario,
            TestStep step,
            String resolvedValue,
            String rawXpath,
            String resolvedXpath,
            String executedByClass,
            String executionSource,
            String evidence,
            String message
    ) {
        return fromStep(scenario, step, resolvedValue, rawXpath, resolvedXpath, executedByClass, executionSource, true, STATUS_PASS, evidence, message);
    }

    public static ExecutionResult failure(
            Scenario scenario,
            TestStep step,
            String resolvedValue,
            String rawXpath,
            String resolvedXpath,
            String executedByClass,
            String executionSource,
            String message
    ) {
        return fromStep(scenario, step, resolvedValue, rawXpath, resolvedXpath, executedByClass, executionSource, false, STATUS_FAIL, "", message);
    }

    public static ExecutionResult skipped(
            Scenario scenario,
            TestStep step,
            String resolvedValue,
            String rawXpath,
            String resolvedXpath,
            String executedByClass,
            String executionSource,
            String evidence,
            String message
    ) {
        return fromStep(scenario, step, resolvedValue, rawXpath, resolvedXpath, executedByClass, executionSource, true, STATUS_SKIP, evidence, message);
    }

    private static ExecutionResult fromStep(
            Scenario scenario,
            TestStep step,
            String resolvedValue,
            String rawXpath,
            String resolvedXpath,
            String executedByClass,
            String executionSource,
            boolean success,
            String status,
            String evidence,
            String message
    ) {
        return new ExecutionResult(
                safe(scenario == null ? "" : scenario.getNo()),
                safe(scenario == null ? "" : scenario.getScenarioName()),
                safe(scenario == null ? "" : scenario.getAction()),
                safe(step == null ? "" : step.getTestcaseName()),
                safe(step == null ? "" : step.getDescription()),
                safe(step == null ? "" : step.getFunction()),
                safe(step == null ? "" : step.getObject()),
                safe(step == null ? "" : step.getApplication()),
                safe(step == null ? "" : step.getValue()),
                safe(resolvedValue),
                safe(rawXpath),
                safe(resolvedXpath),
                safe(executedByClass),
                safe(executionSource),
                success,
                safe(status),
                safe(evidence),
                safe(message),
                step == null ? 0 : step.getExcelRowNumber(),
                step == null ? 0 : step.getStepOrder()
        );
    }

    public String getScenarioNo() {
        return scenarioNo;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public String getScenarioAction() {
        return scenarioAction;
    }

    public String getTestcaseName() {
        return testcaseName;
    }

    public String getDescription() {
        return description;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getApplication() {
        return application;
    }

    public String getRawValue() {
        return rawValue;
    }

    public String getResolvedValue() {
        return resolvedValue;
    }

    public String getRawXpath() {
        return rawXpath;
    }

    public String getResolvedXpath() {
        return resolvedXpath;
    }

    public String getExecutedByClass() {
        return executedByClass;
    }

    public String getExecutionSource() {
        return executionSource;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getStatus() {
        return status;
    }

    public String getEvidence() {
        return evidence;
    }

    public String getMessage() {
        return message;
    }

    public int getExcelRowNumber() {
        return excelRowNumber;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    @Override
    public String toString() {
        return "ExecutionResult{" +
                "scenarioNo='" + scenarioNo + '\'' +
                ", scenarioName='" + scenarioName + '\'' +
                ", scenarioAction='" + scenarioAction + '\'' +
                ", testcaseName='" + testcaseName + '\'' +
                ", description='" + description + '\'' +
                ", functionName='" + functionName + '\'' +
                ", objectName='" + objectName + '\'' +
                ", application='" + application + '\'' +
                ", rawValue='" + rawValue + '\'' +
                ", resolvedValue='" + resolvedValue + '\'' +
                ", rawXpath='" + rawXpath + '\'' +
                ", resolvedXpath='" + resolvedXpath + '\'' +
                ", executedByClass='" + executedByClass + '\'' +
                ", executionSource='" + executionSource + '\'' +
                ", success=" + success +
                ", status='" + status + '\'' +
                ", evidence='" + evidence + '\'' +
                ", message='" + message + '\'' +
                ", excelRowNumber=" + excelRowNumber +
                ", stepOrder=" + stepOrder +
                '}';
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
