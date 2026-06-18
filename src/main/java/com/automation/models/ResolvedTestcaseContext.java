package com.automation.models;

import java.util.List;

public class ResolvedTestcaseContext {

    private final String testcaseName;
    private final String application;
    private final int parentExcelRow;
    private final List<ResolvedStepContext> steps;

    public ResolvedTestcaseContext(
            String testcaseName,
            String application,
            int parentExcelRow,
            List<ResolvedStepContext> steps
    ) {
        this.testcaseName = testcaseName;
        this.application = application;
        this.parentExcelRow = parentExcelRow;
        this.steps = steps == null ? List.of() : List.copyOf(steps);
    }

    public String getTestcaseName() {
        return testcaseName;
    }

    public String getApplication() {
        return application;
    }

    public int getParentExcelRow() {
        return parentExcelRow;
    }

    public List<ResolvedStepContext> getSteps() {
        return steps;
    }
}
