package com.automation.reports;

import java.util.Locale;

public class SensitiveDataMasker {

    public static final String MASK = "****";

    public boolean isSensitive(String rawValue, String objectName, String xpath, String description) {
        return isSensitive(rawValue, objectName, xpath, description, "");
    }

    public boolean isSensitive(String rawValue, String objectName, String xpath, String description, String functionName) {
        String combinedText = String.join(
                " ",
                safe(rawValue),
                safe(objectName),
                safe(xpath),
                safe(description),
                safe(functionName)
        ).toLowerCase(Locale.ROOT);

        return combinedText.contains("password")
                || combinedText.contains("passwd")
                || combinedText.contains("pwd")
                || combinedText.contains("secret")
                || combinedText.contains("token");
    }

    public String maskIfNeeded(
            String value,
            boolean showSensitiveData,
            String rawValue,
            String objectName,
            String xpath,
            String description
    ) {
        return maskIfNeeded(value, showSensitiveData, rawValue, objectName, xpath, description, "");
    }

    public String maskIfNeeded(
            String value,
            boolean showSensitiveData,
            String rawValue,
            String objectName,
            String xpath,
            String description,
            String functionName
    ) {
        if (showSensitiveData || value == null || value.isBlank()) {
            return value == null ? "" : value;
        }
        return isSensitive(rawValue, objectName, xpath, description, functionName) ? MASK : value;
    }

    public String maskRawValueIfNeeded(
            String rawValue,
            boolean showSensitiveData,
            String objectName,
            String xpath,
            String description,
            String functionName
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }
        if (showSensitiveData || looksLikeDataReference(rawValue)) {
            return rawValue;
        }
        return isSensitive(rawValue, objectName, xpath, description, functionName) ? MASK : rawValue;
    }

    private boolean looksLikeDataReference(String value) {
        String trimmedValue = value.trim();
        long dotCount = trimmedValue.chars().filter(character -> character == '.').count();
        return dotCount == 1 && !trimmedValue.startsWith(".") && !trimmedValue.endsWith(".");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
