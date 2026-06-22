package com.automation.exceptions;

import java.util.ArrayList;
import java.util.List;

public class ErrorContext {

    private final List<String> lines = new ArrayList<>();

    public ErrorContext sheet(String sheetName) {
        return add("Sheet", sheetName);
    }

    public ErrorContext row(int rowNumber) {
        if (rowNumber > 0) {
            lines.add("Row: " + rowNumber + ".");
        }
        return this;
    }

    public ErrorContext column(String columnName) {
        return add("Column", columnName);
    }

    public ErrorContext scenarioNo(String scenarioNo) {
        return add("Scenario NO", scenarioNo);
    }

    public ErrorContext scenarioAction(String scenarioAction) {
        return add("Scenario ACTION", scenarioAction);
    }

    public ErrorContext testcase(String testcaseName) {
        return add("Testcase", testcaseName);
    }

    public ErrorContext keyword(String keywordName) {
        return add("Keyword", keywordName);
    }

    public ErrorContext object(String objectName) {
        return add("Object", objectName);
    }

    public ErrorContext application(String application) {
        return add("Application", application);
    }

    public String render() {
        return String.join(System.lineSeparator(), lines);
    }

    private ErrorContext add(String label, String value) {
        String safeValue = value == null ? "" : value.trim();
        if (!safeValue.isBlank()) {
            lines.add(label + ": " + safeValue + ".");
        }
        return this;
    }
}
