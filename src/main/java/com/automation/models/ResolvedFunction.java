package com.automation.models;

public class ResolvedFunction {

    private final String application;
    private final String functionName;
    private final String resolvedClassName;
    private final FunctionSourceType sourceType;
    private final String methodName;

    public ResolvedFunction(
            String application,
            String functionName,
            String resolvedClassName,
            FunctionSourceType sourceType,
            String methodName
    ) {
        this.application = application;
        this.functionName = functionName;
        this.resolvedClassName = resolvedClassName;
        this.sourceType = sourceType;
        this.methodName = methodName;
    }

    public String getApplication() {
        return application;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getResolvedClassName() {
        return resolvedClassName;
    }

    public FunctionSourceType getSourceType() {
        return sourceType;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public String toString() {
        return "ResolvedFunction{" +
                "application='" + application + '\'' +
                ", functionName='" + functionName + '\'' +
                ", resolvedClassName='" + resolvedClassName + '\'' +
                ", sourceType=" + sourceType +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
