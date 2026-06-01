package com.automation.models;

public class TestObject {

    private final String application;
    private final String objectName;
    private final String xpath;
    private final String description;
    private final int excelRowNumber;

    public TestObject(String application, String objectName, String xpath, String description, int excelRowNumber) {
        this.application = application;
        this.objectName = objectName;
        this.xpath = xpath;
        this.description = description;
        this.excelRowNumber = excelRowNumber;
    }

    public String getApplication() {
        return application;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getXpath() {
        return xpath;
    }

    public String getDescription() {
        return description;
    }

    public int getExcelRowNumber() {
        return excelRowNumber;
    }

    @Override
    public String toString() {
        return "TestObject{" +
                "application='" + application + '\'' +
                ", objectName='" + objectName + '\'' +
                ", xpath='" + xpath + '\'' +
                ", description='" + description + '\'' +
                ", excelRowNumber=" + excelRowNumber +
                '}';
    }
}
