package com.example.nrlogs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** JSON response for a query: the NRQL that ran, an optional note, columns and rows. */
public record QueryResult(String nrql, String explanation, List<String> columns,
                          List<Map<String, Object>> rows) {

    public static QueryResult of(String nrql, String explanation, List<Map<String, Object>> rows) {
        Set<String> cols = new LinkedHashSet<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                cols.addAll(row.keySet());
            }
        }
        return new QueryResult(nrql, explanation, new ArrayList<>(cols),
                rows == null ? List.of() : rows);
    }
}
