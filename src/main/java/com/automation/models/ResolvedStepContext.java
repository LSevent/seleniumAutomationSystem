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
    private final String function;
    private final String objectName;
    private final String application;
    private final String description;
    private final String rawValue;
    private final String resolvedValue;
    private final String rawXPath;
    private final String resolvedXPath;
    private final String executedBy;

    public static Builder builder() {
        return new Builder();
    }

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

    public String app() {
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
                ", function='" + function + '\'' +
                ", objectName='" + objectName + '\'' +
                ", application='" + application + '\'' +
                ", description='" + description + '\'' +
                ", rawXPath='" + rawXPath + '\'' +
                ", resolvedXPath='" + resolvedXPath + '\'' +
                ", executedBy='" + executedBy + '\'' +
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
        private String function;
        private String objectName;
        private String application;
        private String description;
        private String rawValue;
        private String resolvedValue;
        private String rawXPath;
        private String resolvedXPath;
        private String executedBy;

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

        public Builder function(String function) {
            this.function = function;
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

        public ResolvedStepContext build() {
            return new ResolvedStepContext(
                    scenarioNo,
                    scenarioAction,
                    scenarioName,
                    sheetName,
                    testcaseName,
                    testcaseParentRow,
                    excelRow,
                    stepNumber,
                    function,
                    objectName,
                    application,
                    description,
                    rawValue,
                    resolvedValue,
                    rawXPath,
                    resolvedXPath,
                    executedBy
            );
        }
    }
}
