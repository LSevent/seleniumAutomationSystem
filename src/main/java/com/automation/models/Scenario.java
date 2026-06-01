package com.automation.models;

public class Scenario {

    private final String no;
    private final boolean run;
    private final String action;
    private final String scenarioName;
    private final String dataRow;
    private final int excelRowNumber;

    public Scenario(String no, boolean run, String action, String scenarioName, String dataRow, int excelRowNumber) {
        this.no = no;
        this.run = run;
        this.action = action;
        this.scenarioName = scenarioName;
        this.dataRow = dataRow;
        this.excelRowNumber = excelRowNumber;
    }

    public String getNo() {
        return no;
    }

    public boolean isRun() {
        return run;
    }

    public String getAction() {
        return action;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public String getDataRow() {
        return dataRow;
    }

    public int getExcelRowNumber() {
        return excelRowNumber;
    }

    @Override
    public String toString() {
        return "Scenario{" +
                "no='" + no + '\'' +
                ", run=" + run +
                ", action='" + action + '\'' +
                ", scenarioName='" + scenarioName + '\'' +
                ", dataRow='" + dataRow + '\'' +
                ", excelRowNumber=" + excelRowNumber +
                '}';
    }
}
