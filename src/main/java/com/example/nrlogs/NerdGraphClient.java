package com.example.nrlogs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Thin client over New Relic's NerdGraph (GraphQL) API for running NRQL queries.
 *
 * <p>The account id is optional: if it is not configured, it is auto-discovered from the API key
 * via NerdGraph. If the key can see exactly one account, that account is used automatically; if it
 * can see several, an error lists them so you can set {@code nrlogs.new-relic.account-id}.</p>
 */
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

    private static final String ACCOUNTS_QUERY = """
            { actor { accounts { id name } } }
            """;

    private final RestClient restClient;

    /** Configured account id, or null to trigger auto-discovery on first use. */
    private volatile Long accountId;

    public NerdGraphClient(NrLogsProperties.NewRelic config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "New Relic API key is not configured. Set 'nrlogs.new-relic.api-key'.");
        }
        this.accountId = config.getAccountId(); // may be null -> auto-discovered later
        this.restClient = RestClient.builder()
                .baseUrl(config.graphqlEndpoint())
                .defaultHeader("API-Key", config.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<Map<String, Object>> runNrql(String nrql) {
        long account = resolveAccountId();

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("accountId", account);
        variables.put("nrql", nrql);

        Map<String, Object> data = execute(NRQL_QUERY, variables);
        Map<String, Object> actor = asMap(data.get("actor"));
        Map<String, Object> accountNode = asMap(actor.get("account"));
        Map<String, Object> nrqlNode = asMap(accountNode.get("nrql"));

        Object results = nrqlNode.get("results");
        if (results instanceof List<?> rows) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> typed = (List<Map<String, Object>>) rows;
            return typed;
        }
        return List.of();
    }

    /**
     * @return the account id to query. Uses the configured id if present; otherwise discovers it
     *         from the API key. The discovered value is cached for subsequent calls.
     */
    long resolveAccountId() {
        Long current = accountId;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (accountId != null) {
                return accountId;
            }
            List<Account> accounts = fetchAccounts();
            if (accounts.isEmpty()) {
                throw new NerdGraphException(
                        "No New Relic account id was configured and the API key cannot see any "
                                + "accounts. Set 'nrlogs.new-relic.account-id' explicitly.");
            }
            if (accounts.size() > 1) {
                StringJoiner sj = new StringJoiner(", ");
                for (Account a : accounts) {
                    sj.add(a.id() + " (" + a.name() + ")");
                }
                throw new NerdGraphException(
                        "No account id configured and the API key can access multiple accounts: "
                                + sj + ". Set 'nrlogs.new-relic.account-id' to one of these.");
            }
            accountId = accounts.get(0).id();
            return accountId;
        }
    }

    private List<Account> fetchAccounts() {
        Map<String, Object> data = execute(ACCOUNTS_QUERY, Map.of());
        Map<String, Object> actor = asMap(data.get("actor"));
        Object accountsObj = actor.get("accounts");
        List<Account> result = new ArrayList<>();
        if (accountsObj instanceof List<?> list) {
            for (Object item : list) {
                Map<String, Object> m = asMap(item);
                Object id = m.get("id");
                if (id instanceof Number n) {
                    Object name = m.get("name");
                    result.add(new Account(n.longValue(), name == null ? "" : name.toString()));
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> execute(String query, Map<String, Object> variables) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        if (!variables.isEmpty()) {
            body.put("variables", variables);
        }

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
        return asMap(response.get("data"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private record Account(long id, String name) {
    }

    public static class NerdGraphException extends RuntimeException {
        public NerdGraphException(String message) {
            super(message);
        }
    }
}
