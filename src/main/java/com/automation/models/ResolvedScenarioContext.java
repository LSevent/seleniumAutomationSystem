package com.automation.models;

import java.util.List;

public class ResolvedScenarioContext {

    private final String scenarioNo;
    private final String scenarioAction;
    private final String scenarioName;
    private final List<ResolvedTestcaseContext> testcases;

    public ResolvedScenarioContext(
            String scenarioNo,
            String scenarioAction,
            String scenarioName,
            List<ResolvedTestcaseContext> testcases
    ) {
        this.scenarioNo = scenarioNo;
        this.scenarioAction = scenarioAction;
        this.scenarioName = scenarioName;
        this.testcases = testcases == null ? List.of() : List.copyOf(testcases);
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

    public List<ResolvedTestcaseContext> getTestcases() {
        return testcases;
    }
}
