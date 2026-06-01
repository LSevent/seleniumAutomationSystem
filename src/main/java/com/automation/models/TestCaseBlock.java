package com.automation.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestCaseBlock {

    private final String scenarioNo;
    private final String scenarioName;
    private final String scenarioAction;
    private final String testcaseName;
    private final boolean run;
    private final String application;
    private final String description;
    private final int excelRowNumber;
    private final List<TestStep> steps;

    public TestCaseBlock(
            String scenarioNo,
            String scenarioName,
            String scenarioAction,
            String testcaseName,
            boolean run,
            String application,
            String description,
            int excelRowNumber
    ) {
        this.scenarioNo = scenarioNo;
        this.scenarioName = scenarioName;
        this.scenarioAction = scenarioAction;
        this.testcaseName = testcaseName;
        this.run = run;
        this.application = application;
        this.description = description;
        this.excelRowNumber = excelRowNumber;
        this.steps = new ArrayList<>();
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

    public boolean isRun() {
        return run;
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

    public List<TestStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public void addStep(TestStep step) {
        steps.add(step);
    }

    @Override
    public String toString() {
        return "TestCaseBlock{" +
                "scenarioNo='" + scenarioNo + '\'' +
                ", scenarioName='" + scenarioName + '\'' +
                ", scenarioAction='" + scenarioAction + '\'' +
                ", testcaseName='" + testcaseName + '\'' +
                ", run=" + run +
                ", application='" + application + '\'' +
                ", description='" + description + '\'' +
                ", excelRowNumber=" + excelRowNumber +
                ", steps=" + steps +
                '}';
    }
}
