package com.automation.models;

public class KeywordExecutionResult {

    private final String application;
    private final String keywordName;
    private final String executedByClass;
    private final KeywordSourceType sourceType;
    private final boolean success;
    private final String message;

    public KeywordExecutionResult(
            String application,
            String keywordName,
            String executedByClass,
            KeywordSourceType sourceType,
            boolean success,
            String message
    ) {
        this.application = application;
        this.keywordName = keywordName;
        this.executedByClass = executedByClass;
        this.sourceType = sourceType;
        this.success = success;
        this.message = message;
    }

    public String getApplication() {
        return application;
    }

    public String getKeywordName() {
        return keywordName;
    }

    public String getExecutedByClass() {
        return executedByClass;
    }

    public KeywordSourceType getSourceType() {
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
        return "KeywordExecutionResult{" +
                "application='" + application + '\'' +
                ", keywordName='" + keywordName + '\'' +
                ", executedByClass='" + executedByClass + '\'' +
                ", sourceType=" + sourceType +
                ", success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
