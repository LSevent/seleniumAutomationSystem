package com.automation.models;

public class ResolvedStepContext {

    private final String scenarioNo;
    private final String scenarioAction;
    private final String scenarioName;
    private final String sheetName;
    private final String testcaseName;
    private final int testcaseParentRow;
    private final int excelRow;
    private final int stepNumber;
    private final String function;
    private final String objectName;
    private final String application;
    private final String description;
    private final String rawValue;
    private final String resolvedValue;
    private final String rawXPath;
    private final String resolvedXPath;
    private final String executedBy;

    public ResolvedStepContext(
            String scenarioNo,
            String scenarioAction,
            String scenarioName,
            String sheetName,
            String testcaseName,
            int testcaseParentRow,
            int excelRow,
            int stepNumber,
            String function,
            String objectName,
            String application,
            String description,
            String rawValue,
            String resolvedValue,
            String rawXPath,
            String resolvedXPath,
            String executedBy
    ) {
        this.scenarioNo = scenarioNo;
        this.scenarioAction = scenarioAction;
        this.scenarioName = scenarioName;
        this.sheetName = sheetName;
        this.testcaseName = testcaseName;
        this.testcaseParentRow = testcaseParentRow;
        this.excelRow = excelRow;
        this.stepNumber = stepNumber;
        this.function = function;
        this.objectName = objectName;
        this.application = application;
        this.description = description;
        this.rawValue = rawValue;
        this.resolvedValue = resolvedValue;
        this.rawXPath = rawXPath;
        this.resolvedXPath = resolvedXPath;
        this.executedBy = executedBy;
    }

    public String getScenarioNo() {
        return scenarioNo;
    }

    public String getScenarioAction() {
        return scenarioAction;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public String getSheetName() {
        return sheetName;
    }

    public String getTestcaseName() {
        return testcaseName;
    }

    public int getTestcaseParentRow() {
        return testcaseParentRow;
    }

    public int getExcelRow() {
        return excelRow;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public String getFunction() {
        return function;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getApplication() {
        return application;
    }

    public String getDescription() {
        return description;
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

    public String getExecutedBy() {
        return executedBy;
    }

    @Override
    public String toString() {
        return "ResolvedStepContext{" +
                "scenarioNo='" + scenarioNo + '\'' +
                ", scenarioAction='" + scenarioAction + '\'' +
                ", scenarioName='" + scenarioName + '\'' +
                ", sheetName='" + sheetName + '\'' +
                ", testcaseName='" + testcaseName + '\'' +
                ", testcaseParentRow=" + testcaseParentRow +
                ", excelRow=" + excelRow +
                ", stepNumber=" + stepNumber +
                ", function='" + function + '\'' +
                ", objectName='" + objectName + '\'' +
                ", application='" + application + '\'' +
                ", description='" + description + '\'' +
                ", rawValue='" + rawValue + '\'' +
                ", resolvedValue='" + resolvedValue + '\'' +
                ", rawXPath='" + rawXPath + '\'' +
                ", resolvedXPath='" + resolvedXPath + '\'' +
                ", executedBy='" + executedBy + '\'' +
                '}';
    }
}
