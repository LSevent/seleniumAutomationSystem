package com.automation.models;

public class TestStep {

    private final String scenarioNo;
    private final String scenarioName;
    private final String scenarioAction;
    private final String testcaseName;
    private final String function;
    private final String object;
    private final String value;
    private final String application;
    private final String description;
    private final int excelRowNumber;
    private final int stepOrder;

    public TestStep(
            String scenarioNo,
            String scenarioName,
            String scenarioAction,
            String testcaseName,
            String function,
            String object,
            String value,
            String application,
            String description,
            int excelRowNumber,
            int stepOrder
    ) {
        this.scenarioNo = scenarioNo;
        this.scenarioName = scenarioName;
        this.scenarioAction = scenarioAction;
        this.testcaseName = testcaseName;
        this.function = function;
        this.object = object;
        this.value = value;
        this.application = application;
        this.description = description;
        this.excelRowNumber = excelRowNumber;
        this.stepOrder = stepOrder;
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

    public String getFunction() {
        return function;
    }

    public String getObject() {
        return object;
    }

    public String getValue() {
        return value;
    }

    public String getApplication() {
        return application;
    }

    public String getDescription() {
        return description;
    }

    public int getExcelRowNumber() {
        return excelRowNumber;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    @Override
    public String toString() {
        return "TestStep{" +
                "scenarioNo='" + scenarioNo + '\'' +
                ", scenarioName='" + scenarioName + '\'' +
                ", scenarioAction='" + scenarioAction + '\'' +
                ", testcaseName='" + testcaseName + '\'' +
                ", function='" + function + '\'' +
                ", object='" + object + '\'' +
                ", value='" + value + '\'' +
                ", application='" + application + '\'' +
                ", description='" + description + '\'' +
                ", excelRowNumber=" + excelRowNumber +
                ", stepOrder=" + stepOrder +
                '}';
    }
}
