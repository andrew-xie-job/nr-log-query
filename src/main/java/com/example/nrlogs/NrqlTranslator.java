package com.example.nrlogs;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;

/** Translates a plain-English question into a single NRQL log query using an available LLM. */
public class NrqlTranslator {

    private static final String SYSTEM_PROMPT = """
            You translate a user's natural-language request into a single New Relic NRQL query
            that reads log data. Accuracy matters more than cleverness.

            Rules:
            - Query the `Log` event type unless the user clearly asks otherwise.
            - Return exactly ONE NRQL statement. No comments, no multiple statements.
            - Always include a SINCE clause. If no time range is given, use: SINCE %s.
            - Always include a LIMIT. If none is given, use: LIMIT %d.
            - For raw rows prefer: SELECT timestamp, level, message, `service.name`, hostname
              and ORDER BY timestamp DESC. Use aggregations (count, etc.) when asked.
            - Attribute names with dots must be back-quoted, e.g. `service.name`.
            - For text matches in messages use: WHERE message LIKE '%%keyword%%'.
            """;

    private final Map<LlmProvider, ChatClient> clients;
    private final LlmProvider defaultProvider;
    private final NrLogsProperties properties;

    public NrqlTranslator(Map<LlmProvider, ChatClient> clients, LlmProvider defaultProvider,
                          NrLogsProperties properties) {
        this.clients = new EnumMap<>(LlmProvider.class);
        this.clients.putAll(clients);
        this.defaultProvider = defaultProvider;
        this.properties = properties;
    }

    public boolean hasProvider() { return !clients.isEmpty(); }

    public NrqlResponse translate(String question) {
        ChatClient client = pickClient();
        NrqlResponse response = client.prompt()
                .system(systemPrompt())
                .user(question)
                .call()
                .entity(NrqlResponse.class);
        if (response == null || response.nrql() == null || response.nrql().isBlank()) {
            throw new IllegalStateException("The model did not return a NRQL query for: " + question);
        }
        return response;
    }

    private ChatClient pickClient() {
        if (defaultProvider != null && clients.containsKey(defaultProvider)) {
            return clients.get(defaultProvider);
        }
        return clients.values().stream().findFirst().orElseThrow(() -> new IllegalStateException(
                "No language model is available to translate English to NRQL. Start a local Ollama "
                        + "server (or configure an OpenAI/Anthropic key), or use the 'NRQL' mode to "
                        + "type a query directly."));
    }

    private String systemPrompt() {
        return SYSTEM_PROMPT.formatted(properties.getDefaultSince(), properties.getDefaultLimit());
    }
}
