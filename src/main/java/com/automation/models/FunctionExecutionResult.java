package com.automation.models;

public class FunctionExecutionResult {

    private final String application;
    private final String functionName;
    private final String executedByClass;
    private final FunctionSourceType sourceType;
    private final boolean success;
    private final String message;

    public FunctionExecutionResult(
            String application,
            String functionName,
            String executedByClass,
            FunctionSourceType sourceType,
            boolean success,
            String message
    ) {
        this.application = application;
        this.functionName = functionName;
        this.executedByClass = executedByClass;
        this.sourceType = sourceType;
        this.success = success;
        this.message = message;
    }

    public String getApplication() {
        return application;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getExecutedByClass() {
        return executedByClass;
    }

    public FunctionSourceType getSourceType() {
        return sourceType;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "FunctionExecutionResult{" +
                "application='" + application + '\'' +
                ", functionName='" + functionName + '\'' +
                ", executedByClass='" + executedByClass + '\'' +
                ", sourceType=" + sourceType +
                ", success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
