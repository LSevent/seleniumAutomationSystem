package com.automation.models;

public final class ResolvedStepContext {

    private final String scenarioNo;
    private final String scenarioAction;
    private final String scenarioName;
    private final String sheetName;
    private final String testcaseName;
    private final int testcaseParentRow;
    private final int excelRow;
    private final int stepNumber;
    private final String keyword;
    private final String objectName;
    private final String application;
    private final String description;
    private final String rawValue;
    private final String resolvedValue;
    private final String rawXPath;
    private final String resolvedXPath;
    private final String executedBy;
    private final FlowDirectiveType flowDirective;

    public static Builder builder() {
        return new Builder();
    }

    private ResolvedStepContext(Builder builder) {
        this.scenarioNo = builder.scenarioNo;
        this.scenarioAction = builder.scenarioAction;
        this.scenarioName = builder.scenarioName;
        this.sheetName = builder.sheetName;
        this.testcaseName = builder.testcaseName;
        this.testcaseParentRow = builder.testcaseParentRow;
        this.excelRow = builder.excelRow;
        this.stepNumber = builder.stepNumber;
        this.keyword = builder.keyword;
        this.objectName = builder.objectName;
        this.application = builder.application;
        this.description = builder.description;
        this.rawValue = builder.rawValue;
        this.resolvedValue = builder.resolvedValue;
        this.rawXPath = builder.rawXPath;
        this.resolvedXPath = builder.resolvedXPath;
        this.executedBy = builder.executedBy;
        this.flowDirective = builder.flowDirective == null
                ? FlowDirectiveType.fromKeyword(builder.keyword)
                : builder.flowDirective;
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

    public String getKeyword() {
        return keyword;
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

    public FlowDirectiveType getFlowDirective() {
        return flowDirective;
    }

    public boolean isFlowDirective() {
        return flowDirective != null && flowDirective != FlowDirectiveType.NONE;
    }

    public boolean isConditionalDirective() {
        return flowDirective != null && flowDirective.isConditional();
    }

    public boolean isLoopDirective() {
        return flowDirective != null && flowDirective.isLoop();
    }

    public String xpath() {
        return resolvedXPath;
    }

    public String value() {
        return resolvedValue;
    }

    public String rawValue() {
        return rawValue;
    }

    public String object() {
        return objectName;
    }

    public String objectName() {
        return objectName;
    }

    public String app() {
        return application;
    }

    public String application() {
        return application;
    }

    public boolean hasXPath() {
        return resolvedXPath != null && !resolvedXPath.isBlank();
    }

    public boolean hasValue() {
        return resolvedValue != null && !resolvedValue.isBlank();
    }

    public boolean hasObject() {
        return objectName != null && !objectName.isBlank();
    }

    public boolean hasApplication() {
        return application != null && !application.isBlank();
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
                ", keyword='" + keyword + '\'' +
                ", objectName='" + objectName + '\'' +
                ", application='" + application + '\'' +
                ", description='" + description + '\'' +
                ", rawXPath='" + rawXPath + '\'' +
                ", resolvedXPath='" + resolvedXPath + '\'' +
                ", executedBy='" + executedBy + '\'' +
                ", flowDirective=" + flowDirective +
                '}';
    }

    public static final class Builder {

        private String scenarioNo;
        private String scenarioAction;
        private String scenarioName;
        private String sheetName;
        private String testcaseName;
        private int testcaseParentRow;
        private int excelRow;
        private int stepNumber;
        private String keyword;
        private String objectName;
        private String application;
        private String description;
        private String rawValue;
        private String resolvedValue;
        private String rawXPath;
        private String resolvedXPath;
        private String executedBy;
        private FlowDirectiveType flowDirective;

        private Builder() {
        }

        public Builder scenarioNo(String scenarioNo) {
            this.scenarioNo = scenarioNo;
            return this;
        }

        public Builder scenarioAction(String scenarioAction) {
            this.scenarioAction = scenarioAction;
            return this;
        }

        public Builder scenarioName(String scenarioName) {
            this.scenarioName = scenarioName;
            return this;
        }

        public Builder sheetName(String sheetName) {
            this.sheetName = sheetName;
            return this;
        }

        public Builder testcaseName(String testcaseName) {
            this.testcaseName = testcaseName;
            return this;
        }

        public Builder testcaseParentRow(int testcaseParentRow) {
            this.testcaseParentRow = testcaseParentRow;
            return this;
        }

        public Builder excelRow(int excelRow) {
            this.excelRow = excelRow;
            return this;
        }

        public Builder stepNumber(int stepNumber) {
            this.stepNumber = stepNumber;
            return this;
        }

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder objectName(String objectName) {
            this.objectName = objectName;
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

        public Builder rawValue(String rawValue) {
            this.rawValue = rawValue;
            return this;
        }

        public Builder resolvedValue(String resolvedValue) {
            this.resolvedValue = resolvedValue;
            return this;
        }

        public Builder rawXPath(String rawXPath) {
            this.rawXPath = rawXPath;
            return this;
        }

        public Builder resolvedXPath(String resolvedXPath) {
            this.resolvedXPath = resolvedXPath;
            return this;
        }

        public Builder executedBy(String executedBy) {
            this.executedBy = executedBy;
            return this;
        }

        public Builder flowDirective(FlowDirectiveType flowDirective) {
            this.flowDirective = flowDirective;
            return this;
        }

        public ResolvedStepContext build() {
            return new ResolvedStepContext(this);
        }
    }
}
