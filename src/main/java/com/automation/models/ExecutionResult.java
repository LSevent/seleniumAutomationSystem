package com.automation.models;

public class ExecutionResult {

    public static final String STATUS_PASS = "PASS";
    public static final String STATUS_FAIL = "FAIL";
    public static final String STATUS_SKIP = "SKIP";

    public static Builder builder() {
        return new Builder();
    }

    public static Builder fromStep(ResolvedStepContext step) {
        return builder()
                .scenarioNo(safe(step == null ? "" : step.getScenarioNo()))
                .scenarioName(safe(step == null ? "" : step.getScenarioName()))
                .scenarioAction(safe(step == null ? "" : step.getScenarioAction()))
                .testcaseName(safe(step == null ? "" : step.getTestcaseName()))
                .description(safe(step == null ? "" : step.getDescription()))
                .keywordName(safe(step == null ? "" : step.getKeyword()))
                .objectName(safe(step == null ? "" : step.getObjectName()))
                .application(safe(step == null ? "" : step.getApplication()))
                .rawValue(safe(step == null ? "" : step.getRawValue()))
                .resolvedValue(safe(step == null ? "" : step.getResolvedValue()))
                .rawXPath(safe(step == null ? "" : step.getRawXPath()))
                .resolvedXPath(safe(step == null ? "" : step.getResolvedXPath()))
                .executedByClass(safe(step == null ? "" : step.getExecutedBy()))
                .excelRowNumber(step == null ? 0 : step.getExcelRow())
                .stepOrder(step == null ? 0 : step.getStepNumber());
    }

    private final String scenarioNo;
    private final String scenarioName;
    private final String scenarioAction;
    private final String testcaseName;
    private final String description;
    private final String keywordName;
    private final String objectName;
    private final String application;
    private final String rawValue;
    private final String resolvedValue;
    private final String rawXPath;
    private final String resolvedXPath;
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
            String keywordName,
            String objectName,
            String application,
            String rawValue,
            String resolvedValue,
            String rawXPath,
            String resolvedXPath,
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
        this.keywordName = keywordName;
        this.objectName = objectName;
        this.application = application;
        this.rawValue = rawValue;
        this.resolvedValue = resolvedValue;
        this.rawXPath = rawXPath;
        this.resolvedXPath = resolvedXPath;
        this.executedByClass = executedByClass;
        this.executionSource = executionSource;
        this.success = success;
        this.status = status;
        this.evidence = evidence;
        this.message = message;
        this.excelRowNumber = excelRowNumber;
        this.stepOrder = stepOrder;
    }

    private ExecutionResult(Builder builder) {
        this.scenarioNo = builder.scenarioNo;
        this.scenarioName = builder.scenarioName;
        this.scenarioAction = builder.scenarioAction;
        this.testcaseName = builder.testcaseName;
        this.description = builder.description;
        this.keywordName = builder.keywordName;
        this.objectName = builder.objectName;
        this.application = builder.application;
        this.rawValue = builder.rawValue;
        this.resolvedValue = builder.resolvedValue;
        this.rawXPath = builder.rawXPath;
        this.resolvedXPath = builder.resolvedXPath;
        this.executedByClass = builder.executedByClass;
        this.executionSource = builder.executionSource;
        this.success = builder.success;
        this.status = builder.status;
        this.evidence = builder.evidence;
        this.message = builder.message;
        this.excelRowNumber = builder.excelRowNumber;
        this.stepOrder = builder.stepOrder;
    }

    public static ExecutionResult success(
            Scenario scenario,
            TestStep step,
            String resolvedValue,
            String rawXPath,
            String resolvedXPath,
            String executedByClass,
            String executionSource,
            String message
    ) {
        return fromStep(scenario, step, resolvedValue, rawXPath, resolvedXPath, executedByClass, executionSource, true, STATUS_PASS, "", message);
    }

    public static ExecutionResult success(
            Scenario scenario,
            TestStep step,
            String resolvedValue,
            String rawXPath,
            String resolvedXPath,
            String executedByClass,
            String executionSource,
            String evidence,
            String message
    ) {
        return fromStep(scenario, step, resolvedValue, rawXPath, resolvedXPath, executedByClass, executionSource, true, STATUS_PASS, evidence, message);
    }

    public static ExecutionResult failure(
            Scenario scenario,
            TestStep step,
            String resolvedValue,
            String rawXPath,
            String resolvedXPath,
            String executedByClass,
            String executionSource,
            String message
    ) {
        return fromStep(scenario, step, resolvedValue, rawXPath, resolvedXPath, executedByClass, executionSource, false, STATUS_FAIL, "", message);
    }

    public static ExecutionResult skipped(
            Scenario scenario,
            TestStep step,
            String resolvedValue,
            String rawXPath,
            String resolvedXPath,
            String executedByClass,
            String executionSource,
            String evidence,
            String message
    ) {
        return fromStep(scenario, step, resolvedValue, rawXPath, resolvedXPath, executedByClass, executionSource, true, STATUS_SKIP, evidence, message);
    }

    public static ExecutionResult success(
            ResolvedStepContext step,
            String executedByClass,
            String executionSource,
            String message
    ) {
        return fromResolvedStep(step, executedByClass, executionSource, true, STATUS_PASS, "", message);
    }

    public static ExecutionResult success(
            ResolvedStepContext step,
            String executedByClass,
            String executionSource,
            String evidence,
            String message
    ) {
        return fromResolvedStep(step, executedByClass, executionSource, true, STATUS_PASS, evidence, message);
    }

    public static ExecutionResult failure(
            ResolvedStepContext step,
            String executedByClass,
            String executionSource,
            String message
    ) {
        return fromResolvedStep(step, executedByClass, executionSource, false, STATUS_FAIL, "", message);
    }

    public static ExecutionResult skipped(
            ResolvedStepContext step,
            String executedByClass,
            String executionSource,
            String evidence,
            String message
    ) {
        return fromResolvedStep(step, executedByClass, executionSource, true, STATUS_SKIP, evidence, message);
    }

    private static ExecutionResult fromStep(
            Scenario scenario,
            TestStep step,
            String resolvedValue,
            String rawXPath,
            String resolvedXPath,
            String executedByClass,
            String executionSource,
            boolean success,
            String status,
            String evidence,
            String message
    ) {
        return builder()
                .scenarioNo(safe(scenario == null ? "" : scenario.getNo()))
                .scenarioName(safe(scenario == null ? "" : scenario.getScenarioName()))
                .scenarioAction(safe(scenario == null ? "" : scenario.getAction()))
                .testcaseName(safe(step == null ? "" : step.getTestcaseName()))
                .description(safe(step == null ? "" : step.getDescription()))
                .keywordName(safe(step == null ? "" : step.getKeyword()))
                .objectName(safe(step == null ? "" : step.getObject()))
                .application(safe(step == null ? "" : step.getApplication()))
                .rawValue(safe(step == null ? "" : step.getValue()))
                .resolvedValue(safe(resolvedValue))
                .rawXPath(safe(rawXPath))
                .resolvedXPath(safe(resolvedXPath))
                .executedByClass(safe(executedByClass))
                .executionSource(safe(executionSource))
                .success(success)
                .status(safe(status))
                .evidence(safe(evidence))
                .message(safe(message))
                .excelRowNumber(step == null ? 0 : step.getExcelRowNumber())
                .stepOrder(step == null ? 0 : step.getStepOrder())
                .build();
    }

    private static ExecutionResult fromResolvedStep(
            ResolvedStepContext step,
            String executedByClass,
            String executionSource,
            boolean success,
            String status,
            String evidence,
            String message
    ) {
        return fromStep(step)
                .executedByClass(safe(executedByClass))
                .executionSource(safe(executionSource))
                .success(success)
                .status(safe(status))
                .evidence(safe(evidence))
                .message(safe(message))
                .build();
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

    public String getKeywordName() {
        return keywordName;
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

    public String getRawXPath() {
        return rawXPath;
    }

    public String getResolvedXPath() {
        return resolvedXPath;
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
                ", keywordName='" + keywordName + '\'' +
                ", objectName='" + objectName + '\'' +
                ", application='" + application + '\'' +
                ", rawXPath='" + rawXPath + '\'' +
                ", resolvedXPath='" + resolvedXPath + '\'' +
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

    public static final class Builder {

        private String scenarioNo;
        private String scenarioName;
        private String scenarioAction;
        private String testcaseName;
        private String description;
        private String keywordName;
        private String objectName;
        private String application;
        private String rawValue;
        private String resolvedValue;
        private String rawXPath;
        private String resolvedXPath;
        private String executedByClass;
        private String executionSource;
        private boolean success;
        private String status;
        private String evidence;
        private String message;
        private int excelRowNumber;
        private int stepOrder;

        private Builder() {
        }

        public Builder scenarioNo(String scenarioNo) {
            this.scenarioNo = scenarioNo;
            return this;
        }

        public Builder scenarioName(String scenarioName) {
            this.scenarioName = scenarioName;
            return this;
        }

        public Builder scenarioAction(String scenarioAction) {
            this.scenarioAction = scenarioAction;
            return this;
        }

        public Builder testcaseName(String testcaseName) {
            this.testcaseName = testcaseName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder keywordName(String keywordName) {
            this.keywordName = keywordName;
            return this;
        }

        public Builder keyword(String keyword) {
            return keywordName(keyword);
        }

        public Builder objectName(String objectName) {
            this.objectName = objectName;
            return this;
        }

        public Builder application(String application) {
            this.application = application;
            return this;
        }

        public Builder rawValue(String rawValue) {
            this.rawValue = rawValue;
            return this;
        }

        public Builder resolvedValue(String resolvedValue) {
            this.resolvedValue = resolvedValue;
            return this;
        }

        public Builder rawXPath(String rawXPath) {
            this.rawXPath = rawXPath;
            return this;
        }

        public Builder resolvedXPath(String resolvedXPath) {
            this.resolvedXPath = resolvedXPath;
            return this;
        }

        public Builder executedByClass(String executedByClass) {
            this.executedByClass = executedByClass;
            return this;
        }

        public Builder executedBy(String executedBy) {
            return executedByClass(executedBy);
        }

        public Builder executionSource(String executionSource) {
            this.executionSource = executionSource;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder evidence(String evidence) {
            this.evidence = evidence;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder excelRowNumber(int excelRowNumber) {
            this.excelRowNumber = excelRowNumber;
            return this;
        }

        public Builder excelRow(int excelRow) {
            return excelRowNumber(excelRow);
        }

        public Builder stepOrder(int stepOrder) {
            this.stepOrder = stepOrder;
            return this;
        }

        public Builder stepNumber(int stepNumber) {
            return stepOrder(stepNumber);
        }

        public ExecutionResult build() {
            return new ExecutionResult(this);
        }
    }
}
