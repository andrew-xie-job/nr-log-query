package com.example.nrlogs;

import java.util.List;
import java.util.Map;

public record LogQueryResult(String nrql, String explanation, List<Map<String, Object>> results) {

    public int size() {
        return results == null ? 0 : results.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NRQL: ").append(nrql).append(System.lineSeparator());
        if (explanation != null && !explanation.isBlank()) {
            sb.append("Note: ").append(explanation).append(System.lineSeparator());
        }
        sb.append(TableFormatter.format(results));
        return sb.toString();
    }
}
