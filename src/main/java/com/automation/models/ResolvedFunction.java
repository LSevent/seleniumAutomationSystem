package com.automation.models;

public class ResolvedFunction {

    private final String application;
    private final String keywordName;
    private final String resolvedClassName;
    private final FunctionSourceType sourceType;
    private final String methodName;

    public ResolvedFunction(
            String application,
            String keywordName,
            String resolvedClassName,
            FunctionSourceType sourceType,
            String methodName
    ) {
        this.application = application;
        this.keywordName = keywordName;
        this.resolvedClassName = resolvedClassName;
        this.sourceType = sourceType;
        this.methodName = methodName;
    }

    public String getApplication() {
        return application;
    }

    public String getKeywordName() {
        return keywordName;
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
                ", keywordName='" + keywordName + '\'' +
                ", resolvedClassName='" + resolvedClassName + '\'' +
                ", sourceType=" + sourceType +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
