package com.automation.engine;

import com.automation.models.ResolvedObject;
import com.automation.models.Scenario;
import com.automation.models.TestCaseBlock;
import com.automation.models.TestStep;

public class ExecutionContext {

    private Scenario scenario;
    private TestCaseBlock testCaseBlock;
    private TestStep testStep;
    private String resolvedValue = "";
    private ResolvedObject resolvedObject;
    private String resolvedXpath = "";
    private String rawXpath = "";
    private String executedByClass = "";
    private String executedBySource = "";
    private String message = "";
    private int currentStepNumber;

    public Scenario getScenario() {
        return scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public TestCaseBlock getTestCaseBlock() {
        return testCaseBlock;
    }

    public void setTestCaseBlock(TestCaseBlock testCaseBlock) {
        this.testCaseBlock = testCaseBlock;
    }

    public TestStep getTestStep() {
        return testStep;
    }

    public void setTestStep(TestStep testStep) {
        this.testStep = testStep;
    }

    public String getResolvedValue() {
        return resolvedValue;
    }

    public void setResolvedValue(String resolvedValue) {
        this.resolvedValue = resolvedValue == null ? "" : resolvedValue;
    }

    public ResolvedObject getResolvedObject() {
        return resolvedObject;
    }

    public void setResolvedObject(ResolvedObject resolvedObject) {
        this.resolvedObject = resolvedObject;
    }

    public String getResolvedXpath() {
        return resolvedXpath;
    }

    public void setResolvedXpath(String resolvedXpath) {
        this.resolvedXpath = resolvedXpath == null ? "" : resolvedXpath;
    }

    public String getRawXpath() {
        return rawXpath;
    }

    public void setRawXpath(String rawXpath) {
        this.rawXpath = rawXpath == null ? "" : rawXpath;
    }

    public String getExecutedByClass() {
        return executedByClass;
    }

    public void setExecutedByClass(String executedByClass) {
        this.executedByClass = executedByClass == null ? "" : executedByClass;
    }

    public String getExecutedBySource() {
        return executedBySource;
    }

    public void setExecutedBySource(String executedBySource) {
        this.executedBySource = executedBySource == null ? "" : executedBySource;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? "" : message;
    }

    public int getCurrentStepNumber() {
        return currentStepNumber;
    }

    public void setCurrentStepNumber(int currentStepNumber) {
        this.currentStepNumber = currentStepNumber;
    }
}
