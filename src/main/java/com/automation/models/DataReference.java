package com.automation.models;

public class DataReference {

    private final String rawReference;
    private final String sheetName;
    private final String columnName;

    public DataReference(String rawReference, String sheetName, String columnName) {
        this.rawReference = rawReference;
        this.sheetName = sheetName;
        this.columnName = columnName;
    }

    public String getRawReference() {
        return rawReference;
    }

    public String getSheetName() {
        return sheetName;
    }

    public String getColumnName() {
        return columnName;
    }

    @Override
    public String toString() {
        return "DataReference{" +
                "rawReference='" + rawReference + '\'' +
                ", sheetName='" + sheetName + '\'' +
                ", columnName='" + columnName + '\'' +
                '}';
    }
}
