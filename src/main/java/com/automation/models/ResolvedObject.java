package com.automation.models;

public class ResolvedObject {

    private final String originalObjectName;
    private final String application;
    private final String rawXPath;
    private final String resolvedXPath;
    private final String rawValue;
    private final String resolvedValue;
    private final int excelRowNumber;

    public ResolvedObject(
            String originalObjectName,
            String application,
            String rawXPath,
            String resolvedXPath,
            String rawValue,
            String resolvedValue,
            int excelRowNumber
    ) {
        this.originalObjectName = originalObjectName;
        this.application = application;
        this.rawXPath = rawXPath;
        this.resolvedXPath = resolvedXPath;
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

    public String getRawXPath() {
        return rawXPath;
    }

    public String getResolvedXPath() {
        return resolvedXPath;
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
                ", rawXPath='" + rawXPath + '\'' +
                ", resolvedXPath='" + resolvedXPath + '\'' +
                ", excelRowNumber=" + excelRowNumber +
                '}';
    }
}
