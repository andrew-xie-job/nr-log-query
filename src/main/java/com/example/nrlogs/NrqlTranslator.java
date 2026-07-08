package com.example.nrlogs;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;

public class NrqlTranslator {

    private static final String SYSTEM_PROMPT = """
            You translate a developer's natural-language request into a single New Relic NRQL query
            that reads log data. You are embedded in a debugging tool, so accuracy matters more than
            cleverness.

            Rules:
            - Query the `Log` event type unless the user clearly asks for a different event type.
            - Return exactly ONE NRQL statement. Never return multiple statements or comments.
            - Always include a SINCE clause. If the user does not give a time range, use: SINCE %s.
            - Always include a LIMIT. If the user does not specify one, use: LIMIT %d.
            - Prefer selecting useful log fields: SELECT timestamp, level, message, service.name, hostname
              unless the user asks for an aggregation (count, etc.).
            - Order the most recent logs first when selecting raw rows: use `ORDER BY timestamp DESC` when appropriate.
            - Common attributes: message, level (e.g. 'ERROR','WARN','INFO'), timestamp, hostname,
              service.name, entity.name, trace.id, span.id. Attribute names with dots must be
              back-quoted, e.g. `service.name`.
            - For text matches inside messages, prefer: WHERE message LIKE '%%keyword%%'.
            %s

            Return the query and a short one-sentence explanation of what it does.
            """;

    private final Map<LlmProvider, ChatClient> clients;
    private final LlmProvider defaultProvider;
    private final NrLogsProperties properties;

    public NrqlTranslator(Map<LlmProvider, ChatClient> clients,
                          LlmProvider defaultProvider,
                          NrLogsProperties properties) {
        this.clients = new EnumMap<>(clients);
        this.defaultProvider = defaultProvider;
        this.properties = properties;
    }

    public NrqlResponse translate(String naturalLanguage) {
        return translate(naturalLanguage, defaultProvider);
    }

    public NrqlResponse translate(String naturalLanguage, LlmProvider provider) {
        ChatClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalStateException("LLM provider " + provider
                    + " is not configured. Ensure the matching Spring AI API key is set "
                    + "(spring.ai.openai.api-key / spring.ai.anthropic.api-key).");
        }
        String system = SYSTEM_PROMPT.formatted(
                properties.getDefaultSince(),
                properties.getDefaultLimit(),
                appNameHint());

        NrqlResponse response = client.prompt()
                .system(system)
                .user(naturalLanguage)
                .call()
                .entity(NrqlResponse.class);

        if (response == null || response.nrql() == null || response.nrql().isBlank()) {
            throw new IllegalStateException("LLM did not produce a NRQL query for request: " + naturalLanguage);
        }
        return response;
    }

    private String appNameHint() {
        String appName = properties.getAppName();
        if (appName == null || appName.isBlank()) {
            return "- If the user refers to \"this app\" or \"this service\" but no app name is known,"
                    + " do not invent a service filter.";
        }
        return "- The current application is '" + appName + "'. When the user refers to \"this app\","
                + " \"this service\", or \"here\", filter with WHERE `service.name` = '" + appName + "'.";
    }
}
