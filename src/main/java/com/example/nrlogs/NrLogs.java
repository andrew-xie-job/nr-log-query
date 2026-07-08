package com.example.nrlogs;

/**
 * Static entry point for use from the IntelliJ debugger (Evaluate Expression, Alt+F8).
 *
 * <p>No API keys? Use manual "Kiro" mode:</p>
 * <pre>{@code
 * NrLogs.prompt("errors from checkout in the last 10 minutes")  // prints a prompt to paste into Kiro
 * NrLogs.nrql("<the NRQL Kiro returns>")                        // runs it against New Relic
 * }</pre>
 *
 * <p>Or automatic mode when a provider is available (local Ollama, or an OpenAI/Anthropic key):</p>
 * <pre>{@code
 * NrLogs.ask("errors from checkout in the last 10 minutes")
 * }</pre>
 */
public final class NrLogs {

    private static volatile NrLogQueryService service;

    private NrLogs() {
    }

    static void bind(NrLogQueryService instance) {
        service = instance;
    }

    /** Automatic mode using the default provider (Ollama / OpenAI / Anthropic if configured). */
    public static LogQueryResult ask(String naturalLanguage) {
        return require().ask(naturalLanguage);
    }

    /** Automatic mode using a specific provider ("openai", "anthropic", or "ollama"). */
    public static LogQueryResult ask(String naturalLanguage, String provider) {
        return require().ask(naturalLanguage, LlmProvider.from(provider));
    }

    /** Manual Kiro mode: returns a prompt to paste into the Kiro chat. No API key needed. */
    public static TranslationPrompt prompt(String naturalLanguage) {
        return require().buildPrompt(naturalLanguage);
    }

    /** Translate to NRQL without executing (automatic mode). */
    public static NrqlResponse preview(String naturalLanguage, String provider) {
        return require().preview(naturalLanguage, LlmProvider.from(provider));
    }

    /** Run raw NRQL directly (use with manual Kiro mode, or any hand-written NRQL). */
    public static LogQueryResult nrql(String nrql) {
        return require().runNrql(nrql);
    }

    private static NrLogQueryService require() {
        NrLogQueryService current = service;
        if (current == null) {
            throw new IllegalStateException(
                    "NrLogs is not initialized yet. It is populated once the Spring application "
                            + "context has started and the nr-log-query auto-configuration has run.");
        }
        return current;
    }
}
