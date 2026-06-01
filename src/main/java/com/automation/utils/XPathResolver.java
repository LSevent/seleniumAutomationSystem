package com.automation.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XPathResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]*)}");

    public boolean hasPlaceholder(String xpath) {
        return xpath != null && PLACEHOLDER_PATTERN.matcher(xpath).find();
    }

    public List<String> extractPlaceholders(String xpath) {
        List<String> placeholders = new ArrayList<>();
        if (xpath == null || xpath.isBlank()) {
            return placeholders;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(xpath);
        while (matcher.find()) {
            placeholders.add(matcher.group(1).trim());
        }
        return placeholders;
    }

    public String replacePlaceholder(String xpath, String placeholderName, String resolvedValue) {
        if (xpath == null) {
            return "";
        }
        String placeholder = "{" + placeholderName + "}";
        return xpath.replace(placeholder, resolvedValue == null ? "" : resolvedValue);
    }
}
