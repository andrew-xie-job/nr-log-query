package com.example.nrlogs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class TableFormatter {

    private static final int MAX_CELL_WIDTH = 80;

    private TableFormatter() {
    }

    static String format(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "(no results)";
        }

        Set<String> columns = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            columns.addAll(row.keySet());
        }
        List<String> headers = new ArrayList<>(columns);

        int[] widths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            widths[i] = headers.get(i).length();
        }
        List<List<String>> renderedRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<String> cells = new ArrayList<>(headers.size());
            for (int i = 0; i < headers.size(); i++) {
                String cell = stringify(row.get(headers.get(i)));
                cells.add(cell);
                widths[i] = Math.min(MAX_CELL_WIDTH, Math.max(widths[i], cell.length()));
            }
            renderedRows.add(cells);
        }

        StringBuilder sb = new StringBuilder();
        appendSeparator(sb, widths);
        appendRow(sb, headers, widths);
        appendSeparator(sb, widths);
        for (List<String> cells : renderedRows) {
            appendRow(sb, cells, widths);
        }
        appendSeparator(sb, widths);
        sb.append(rows.size()).append(rows.size() == 1 ? " row" : " rows");
        return sb.toString();
    }

    private static void appendRow(StringBuilder sb, List<String> cells, int[] widths) {
        sb.append('|');
        for (int i = 0; i < widths.length; i++) {
            String cell = i < cells.size() ? cells.get(i) : "";
            if (cell.length() > widths[i]) {
                cell = cell.substring(0, widths[i] - 1) + "\u2026";
            }
            sb.append(' ').append(pad(cell, widths[i])).append(" |");
        }
        sb.append(System.lineSeparator());
    }

    private static void appendSeparator(StringBuilder sb, int[] widths) {
        sb.append('+');
        for (int width : widths) {
            sb.append("-".repeat(width + 2)).append('+');
        }
        sb.append(System.lineSeparator());
    }

    private static String pad(String value, int width) {
        if (value.length() >= width) {
            return value;
        }
        return value + " ".repeat(width - value.length());
    }

    private static String stringify(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).replace("\n", " ").replace("\r", " ");
    }
}
