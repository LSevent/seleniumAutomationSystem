package com.automation.models;

import java.util.Locale;

public enum FlowDirectiveType {
    NONE,
    IF_EQUALS,
    ELSE_IF_EQUALS,
    ELSE,
    END_IF;

    public static FlowDirectiveType fromKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return NONE;
        }

        return switch (keyword.trim().toLowerCase(Locale.ROOT)) {
            case "ifequals" -> IF_EQUALS;
            case "elseifequals" -> ELSE_IF_EQUALS;
            case "else" -> ELSE;
            case "endif" -> END_IF;
            default -> NONE;
        };
    }

    public boolean isConditional() {
        return this != NONE;
    }

    public boolean startsConditionalBlock() {
        return this == IF_EQUALS;
    }

    public boolean isConditionalBranch() {
        return this == ELSE_IF_EQUALS || this == ELSE;
    }

    public boolean endsConditionalBlock() {
        return this == END_IF;
    }
}
