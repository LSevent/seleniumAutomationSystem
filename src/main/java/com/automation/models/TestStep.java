package com.automation.models;

public class TestStep {

    public static Builder builder() {
        return new Builder();
    }

    private final String scenarioNo;
    private final String scenarioName;
    private final String scenarioAction;
    private final String testcaseName;
    private final String keyword;
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
            String keyword,
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
        this.keyword = keyword;
        this.object = object;
        this.value = value;
        this.application = application;
        this.description = description;
        this.excelRowNumber = excelRowNumber;
        this.stepOrder = stepOrder;
    }

    private TestStep(Builder builder) {
        this.scenarioNo = builder.scenarioNo;
        this.scenarioName = builder.scenarioName;
        this.scenarioAction = builder.scenarioAction;
        this.testcaseName = builder.testcaseName;
        this.keyword = builder.keyword;
        this.object = builder.object;
        this.value = builder.value;
        this.application = builder.application;
        this.description = builder.description;
        this.excelRowNumber = builder.excelRowNumber;
        this.stepOrder = builder.stepOrder;
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

    public String getKeyword() {
        return keyword;
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
                ", keyword='" + keyword + '\'' +
                ", object='" + object + '\'' +
                ", application='" + application + '\'' +
                ", description='" + description + '\'' +
                ", excelRowNumber=" + excelRowNumber +
                ", stepOrder=" + stepOrder +
                '}';
    }

    public static final class Builder {

        private String scenarioNo;
        private String scenarioName;
        private String scenarioAction;
        private String testcaseName;
        private String keyword;
        private String object;
        private String value;
        private String application;
        private String description;
        private int excelRowNumber;
        private int stepOrder;

        private Builder() {
        }

        public Builder scenarioNo(String scenarioNo) {
            this.scenarioNo = scenarioNo;
            return this;
        }

        public Builder scenarioName(String scenarioName) {
            this.scenarioName = scenarioName;
            return this;
        }

        public Builder scenarioAction(String scenarioAction) {
            this.scenarioAction = scenarioAction;
            return this;
        }

        public Builder testcaseName(String testcaseName) {
            this.testcaseName = testcaseName;
            return this;
        }

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder object(String object) {
            this.object = object;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder application(String application) {
            this.application = application;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder excelRowNumber(int excelRowNumber) {
            this.excelRowNumber = excelRowNumber;
            return this;
        }

        public Builder rowNumber(int rowNumber) {
            return excelRowNumber(rowNumber);
        }

        public Builder stepOrder(int stepOrder) {
            this.stepOrder = stepOrder;
            return this;
        }

        public TestStep build() {
            return new TestStep(this);
        }
    }
}
