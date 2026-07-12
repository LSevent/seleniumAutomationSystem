package com.automation.models;

import java.util.Locale;

public enum FlowDirectiveType {
    NONE,
    IF_EQUALS,
    ELSE_IF_EQUALS,
    IF_DISPLAYED,
    ELSE_IF_DISPLAYED,
    ELSE,
    END_IF,
    FOR_EACH_DATA_ROW,
    END_FOR_EACH_DATA_ROW;

    public static FlowDirectiveType fromKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return NONE;
        }

        return switch (keyword.trim().toLowerCase(Locale.ROOT)) {
            case "ifequals" -> IF_EQUALS;
            case "elseifequals" -> ELSE_IF_EQUALS;
            case "ifdisplayed" -> IF_DISPLAYED;
            case "elseifdisplayed" -> ELSE_IF_DISPLAYED;
            case "else" -> ELSE;
            case "endif" -> END_IF;
            case "foreachdatarow" -> FOR_EACH_DATA_ROW;
            case "endforeachdatarow" -> END_FOR_EACH_DATA_ROW;
            default -> NONE;
        };
    }

    public boolean isConditional() {
        return this == IF_EQUALS
                || this == ELSE_IF_EQUALS
                || this == IF_DISPLAYED
                || this == ELSE_IF_DISPLAYED
                || this == ELSE
                || this == END_IF;
    }

    public boolean isLoop() {
        return this == FOR_EACH_DATA_ROW
                || this == END_FOR_EACH_DATA_ROW;
    }

    public boolean isFlowDirective() {
        return this != NONE;
    }

    public boolean startsConditionalBlock() {
        return this == IF_EQUALS || this == IF_DISPLAYED;
    }

    public boolean isConditionalBranch() {
        return this == ELSE_IF_EQUALS || this == ELSE_IF_DISPLAYED || this == ELSE;
    }

    public boolean isEqualityCondition() {
        return this == IF_EQUALS || this == ELSE_IF_EQUALS;
    }

    public boolean isVisibilityCondition() {
        return this == IF_DISPLAYED || this == ELSE_IF_DISPLAYED;
    }

    public boolean endsConditionalBlock() {
        return this == END_IF;
    }

    public boolean startsLoopBlock() {
        return this == FOR_EACH_DATA_ROW;
    }

    public boolean endsLoopBlock() {
        return this == END_FOR_EACH_DATA_ROW;
    }
}
