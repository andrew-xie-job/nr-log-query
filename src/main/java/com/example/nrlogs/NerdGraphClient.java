package com.example.nrlogs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class NerdGraphClient {

    private static final String NRQL_QUERY = """
            query($accountId: Int!, $nrql: Nrql!) {
              actor {
                account(id: $accountId) {
                  nrql(query: $nrql) {
                    results
                  }
                }
              }
            }
            """;

    private final RestClient restClient;
    private final long accountId;

    public NerdGraphClient(NrLogsProperties.NewRelic config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "New Relic API key is not configured. Set 'nrlogs.new-relic.api-key'.");
        }
        if (config.getAccountId() == null) {
            throw new IllegalStateException(
                    "New Relic account id is not configured. Set 'nrlogs.new-relic.account-id'.");
        }
        this.accountId = config.getAccountId();
        this.restClient = RestClient.builder()
                .baseUrl(config.graphqlEndpoint())
                .defaultHeader("API-Key", config.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> runNrql(String nrql) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("accountId", accountId);
        variables.put("nrql", nrql);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", NRQL_QUERY);
        body.put("variables", variables);

        Map<String, Object> response = restClient.post()
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new NerdGraphException("Empty response from NerdGraph");
        }

        Object errors = response.get("errors");
        if (errors instanceof List<?> errorList && !errorList.isEmpty()) {
            throw new NerdGraphException("NerdGraph returned errors: " + errorList);
        }

        Map<String, Object> data = asMap(response.get("data"));
        Map<String, Object> actor = asMap(data.get("actor"));
        Map<String, Object> account = asMap(actor.get("account"));
        Map<String, Object> nrqlNode = asMap(account.get("nrql"));

        Object results = nrqlNode.get("results");
        if (results instanceof List<?> rows) {
            return (List<Map<String, Object>>) rows;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    public static class NerdGraphException extends RuntimeException {
        public NerdGraphException(String message) {
            super(message);
        }
    }
}
