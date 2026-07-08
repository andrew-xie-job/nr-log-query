package com.example.nrlogs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client over New Relic NerdGraph (GraphQL). Runs NRQL for a chosen account and lists the
 * accounts the API key can access (used to populate the UI dropdown).
 */
@Component
public class NerdGraphClient {

    private static final String NRQL_QUERY = """
            query($accountId: Int!, $nrql: Nrql!) {
              actor { account(id: $accountId) { nrql(query: $nrql) { results } } }
            }
            """;

    private static final String ACCOUNTS_QUERY = "{ actor { accounts { id name } } }";

    private final NrLogsProperties.NewRelic config;
    private final RestClient restClient;

    public NerdGraphClient(NrLogsProperties properties) {
        this.config = properties.getNewRelic();
        this.restClient = RestClient.builder()
                .baseUrl(config.graphqlEndpoint())
                .defaultHeader("API-Key", config.getApiKey() == null ? "" : config.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** Accounts for the dropdown: explicit config if present, else auto-discovered from the key. */
    public List<AccountInfo> listAccounts() {
        if (config.getAccounts() != null && !config.getAccounts().isEmpty()) {
            List<AccountInfo> out = new ArrayList<>();
            for (NrLogsProperties.Account a : config.getAccounts()) {
                if (a.getId() != null) {
                    out.add(new AccountInfo(a.getId(), a.getName() == null ? String.valueOf(a.getId()) : a.getName()));
                }
            }
            return out;
        }
        requireApiKey();
        Map<String, Object> data = execute(ACCOUNTS_QUERY, Map.of());
        Map<String, Object> actor = asMap(data.get("actor"));
        List<AccountInfo> out = new ArrayList<>();
        if (actor.get("accounts") instanceof List<?> list) {
            for (Object item : list) {
                Map<String, Object> m = asMap(item);
                if (m.get("id") instanceof Number n) {
                    Object name = m.get("name");
                    out.add(new AccountInfo(n.longValue(), name == null ? String.valueOf(n) : name.toString()));
                }
            }
        }
        return out;
    }

    public List<Map<String, Object>> runNrql(long accountId, String nrql) {
        requireApiKey();
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("accountId", accountId);
        variables.put("nrql", nrql);

        Map<String, Object> data = execute(NRQL_QUERY, variables);
        Map<String, Object> actor = asMap(data.get("actor"));
        Map<String, Object> account = asMap(actor.get("account"));
        Map<String, Object> nrqlNode = asMap(account.get("nrql"));
        if (nrqlNode.get("results") instanceof List<?> rows) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> typed = (List<Map<String, Object>>) rows;
            return typed;
        }
        return List.of();
    }

    private void requireApiKey() {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new NerdGraphException("New Relic API key is not set. Provide NEW_RELIC_API_KEY "
                    + "(or nrlogs.new-relic.api-key).");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> execute(String query, Map<String, Object> variables) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        if (!variables.isEmpty()) {
            body.put("variables", variables);
        }
        Map<String, Object> response = restClient.post().body(body).retrieve().body(Map.class);
        if (response == null) {
            throw new NerdGraphException("Empty response from NerdGraph");
        }
        if (response.get("errors") instanceof List<?> errs && !errs.isEmpty()) {
            throw new NerdGraphException("NerdGraph error: " + errs);
        }
        return asMap(response.get("data"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    public static class NerdGraphException extends RuntimeException {
        public NerdGraphException(String message) { super(message); }
    }
}
