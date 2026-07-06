package com.automation.excel;

import org.apache.poi.ss.util.CellReference;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FormulaHeaderReference {

    private static final int HEADER_ROW_NUMBER = 1;
    private static final Pattern CELL_REFERENCE_PATTERN = Pattern.compile("\\$?([A-Za-z]+)\\$?(\\d+)");

    private final String rawFormula;
    private final String sheetName;
    private final int columnIndex;

    private FormulaHeaderReference(String rawFormula, String sheetName, int columnIndex) {
        this.rawFormula = rawFormula;
        this.sheetName = sheetName;
        this.columnIndex = columnIndex;
    }

    static Optional<FormulaHeaderReference> parseValue(String value) {
        String formula = value == null ? "" : value.trim();
        if (!formula.startsWith("=")) {
            return Optional.empty();
        }
        return parseFormula(formula.substring(1));
    }

    static Optional<FormulaHeaderReference> parseCellFormula(String value) {
        return parseFormula(value);
    }

    private static Optional<FormulaHeaderReference> parseFormula(String value) {
        String formula = value == null ? "" : value.trim();
        if (formula.isBlank()) {
            return Optional.empty();
        }

        int sheetSeparatorIndex = formula.lastIndexOf('!');
        if (sheetSeparatorIndex <= 0 || sheetSeparatorIndex == formula.length() - 1) {
            return Optional.empty();
        }

        String sheetToken = formula.substring(0, sheetSeparatorIndex).trim();
        String cellToken = formula.substring(sheetSeparatorIndex + 1).trim();
        String sheetName = unquoteSheetName(sheetToken);
        if (sheetName.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = CELL_REFERENCE_PATTERN.matcher(cellToken);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        int rowNumber;
        try {
            rowNumber = Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
        if (rowNumber != HEADER_ROW_NUMBER) {
            return Optional.empty();
        }

        int columnIndex = CellReference.convertColStringToIndex(matcher.group(1));
        return Optional.of(new FormulaHeaderReference(toRawFormula(formula), sheetName, columnIndex));
    }

    String rawFormula() {
        return rawFormula;
    }

    String sheetName() {
        return sheetName;
    }

    int columnIndex() {
        return columnIndex;
    }

    private static String unquoteSheetName(String sheetToken) {
        if (sheetToken.length() >= 2 && sheetToken.startsWith("'") && sheetToken.endsWith("'")) {
            return sheetToken.substring(1, sheetToken.length() - 1).replace("''", "'");
        }
        return sheetToken;
    }

    private static String toRawFormula(String formula) {
        return "=" + formula;
    }
}
