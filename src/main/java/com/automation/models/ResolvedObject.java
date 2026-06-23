package com.automation.models;

public class ResolvedObject {

    private final String originalObjectName;
    private final String application;
    private final String rawXpath;
    private final String resolvedXpath;
    private final String rawValue;
    private final String resolvedValue;
    private final int excelRowNumber;

    public ResolvedObject(
            String originalObjectName,
            String application,
            String rawXpath,
            String resolvedXpath,
            String rawValue,
            String resolvedValue,
            int excelRowNumber
    ) {
        this.originalObjectName = originalObjectName;
        this.application = application;
        this.rawXpath = rawXpath;
        this.resolvedXpath = resolvedXpath;
        this.rawValue = rawValue;
        this.resolvedValue = resolvedValue;
        this.excelRowNumber = excelRowNumber;
    }

    public String getOriginalObjectName() {
        return originalObjectName;
    }

    public String getApplication() {
        return application;
    }

    public String getRawXpath() {
        return rawXpath;
    }

    public String getResolvedXpath() {
        return resolvedXpath;
    }

    public String getRawValue() {
        return rawValue;
    }

    public String getResolvedValue() {
        return resolvedValue;
    }

    public int getExcelRowNumber() {
        return excelRowNumber;
    }

    @Override
    public String toString() {
        return "ResolvedObject{" +
                "originalObjectName='" + originalObjectName + '\'' +
                ", application='" + application + '\'' +
                ", rawXpath='" + rawXpath + '\'' +
                ", resolvedXpath='" + resolvedXpath + '\'' +
                ", excelRowNumber=" + excelRowNumber +
                '}';
    }
}
