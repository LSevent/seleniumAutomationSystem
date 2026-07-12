package com.automation.models;

import com.automation.exceptions.FrameworkException;

public final class ConditionExpression {

    private final String rawExpression;
    private final String leftOperand;
    private final String rightOperand;

    private ConditionExpression(String rawExpression, String leftOperand, String rightOperand) {
        this.rawExpression = rawExpression;
        this.leftOperand = leftOperand;
        this.rightOperand = rightOperand;
    }

    public static ConditionExpression parse(String expression) {
        String raw = expression == null ? "" : expression.trim();
        if (raw.isBlank()) {
            throw invalidExpression(raw);
        }

        int searchStartIndex = raw.startsWith("=") ? 1 : 0;
        int operatorIndex = raw.indexOf("==", searchStartIndex);
        int operatorLength = 2;
        if (operatorIndex < 0) {
            operatorIndex = raw.indexOf('=', searchStartIndex);
            operatorLength = 1;
        }
        if (operatorIndex < 0) {
            throw invalidExpression(raw);
        }

        String left = raw.substring(0, operatorIndex).trim();
        String right = raw.substring(operatorIndex + operatorLength).trim();
        if (left.isBlank() || right.isBlank()) {
            throw invalidExpression(raw);
        }

        return new ConditionExpression(raw, left, right);
    }

    public static boolean hasComparisonOperator(String expression) {
        String raw = expression == null ? "" : expression.trim();
        if (raw.isBlank()) {
            return false;
        }
        int searchStartIndex = raw.startsWith("=") ? 1 : 0;
        return raw.indexOf("==", searchStartIndex) >= 0
                || raw.indexOf('=', searchStartIndex) >= 0;
    }

    public String getRawExpression() {
        return rawExpression;
    }

    public String getLeftOperand() {
        return leftOperand;
    }

    public String getRightOperand() {
        return rightOperand;
    }

    @Override
    public String toString() {
        return "ConditionExpression{rawExpression='****'}";
    }

    private static FrameworkException invalidExpression(String expression) {
        return new FrameworkException(
                "Invalid condition expression"
                        + (expression == null || expression.isBlank() ? "" : ": " + expression)
                        + ". Expected format: ACTUAL = EXPECTED."
        );
    }
}
