package com.example.nrlogs;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;

/**
 * Translates natural-language questions into NRQL log queries.
 *
 * <p>Supports zero or more automatic providers (OpenAI / Anthropic / Ollama). When none are
 * configured, {@link #buildManualPrompt(String)} still works, powering the manual "Kiro" mode
 * that needs no API key.</p>
 */
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
            """;

    private final Map<LlmProvider, ChatClient> clients;
    private final LlmProvider defaultProvider;
    private final NrLogsProperties properties;

    public NrqlTranslator(Map<LlmProvider, ChatClient> clients,
                          LlmProvider defaultProvider,
                          NrLogsProperties properties) {
        this.clients = new EnumMap<>(LlmProvider.class);
        this.clients.putAll(clients);
        this.defaultProvider = defaultProvider;
        this.properties = properties;
    }

    /** @return true if at least one automatic provider (OpenAI/Anthropic/Ollama) is available. */
    public boolean hasAutomaticProvider() {
        return !clients.isEmpty();
    }

    public NrqlResponse translate(String naturalLanguage) {
        if (defaultProvider != null && clients.containsKey(defaultProvider)) {
            return translate(naturalLanguage, defaultProvider);
        }
        LlmProvider any = clients.keySet().stream().findFirst().orElseThrow(this::noProviderError);
        return translate(naturalLanguage, any);
    }

    public NrqlResponse translate(String naturalLanguage, LlmProvider provider) {
        ChatClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalStateException("LLM provider " + provider
                    + " is not available. Available automatic providers: " + clients.keySet()
                    + ". With no API keys, use manual Kiro mode: NrLogs.prompt(\"" + naturalLanguage + "\").");
        }
        NrqlResponse response = client.prompt()
                .system(systemPrompt())
                .user(naturalLanguage)
                .call()
                .entity(NrqlResponse.class);
        if (response == null || response.nrql() == null || response.nrql().isBlank()) {
            throw new IllegalStateException("LLM did not produce a NRQL query for request: " + naturalLanguage);
        }
        return response;
    }

    /**
     * Builds a self-contained prompt for manual translation via the Kiro chat. No API key needed:
     * paste the result into Kiro, then run the returned NRQL with {@link NrLogs#nrql(String)}.
     */
    public String buildManualPrompt(String naturalLanguage) {
        String nl = System.lineSeparator();
        return systemPrompt()
                + nl
                + "Return ONLY the NRQL query on a single line. No explanation, no markdown, no backticks."
                + nl + nl
                + "Request: " + naturalLanguage;
    }

    private String systemPrompt() {
        return SYSTEM_PROMPT.formatted(
                properties.getDefaultSince(),
                properties.getDefaultLimit(),
                appNameHint());
    }

    private IllegalStateException noProviderError() {
        return new IllegalStateException(
                "No automatic LLM provider is configured. Options with no OpenAI/Anthropic key: "
                        + "(1) run Ollama locally and add spring-ai-starter-model-ollama, or "
                        + "(2) use manual Kiro mode via NrLogs.prompt(\"your question\").");
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
