package com.example.nrlogs;

/**
 * Orchestrates translation + execution. Also exposes manual "Kiro" prompt building and raw NRQL.
 */
public class NrLogQueryService {

    private final NrqlTranslator translator;
    private final NerdGraphClient nerdGraphClient;

    public NrLogQueryService(NrqlTranslator translator, NerdGraphClient nerdGraphClient) {
        this.translator = translator;
        this.nerdGraphClient = nerdGraphClient;
    }

    /** Automatic mode: needs an OpenAI/Anthropic key or a local Ollama. */
    public LogQueryResult ask(String naturalLanguage) {
        return execute(translator.translate(naturalLanguage));
    }

    public LogQueryResult ask(String naturalLanguage, LlmProvider provider) {
        return execute(translator.translate(naturalLanguage, provider));
    }

    /** Manual (Kiro) mode: returns a prompt to paste into the Kiro chat. No API key needed. */
    public TranslationPrompt buildPrompt(String naturalLanguage) {
        return new TranslationPrompt(translator.buildManualPrompt(naturalLanguage));
    }

    public NrqlResponse preview(String naturalLanguage, LlmProvider provider) {
        return translator.translate(naturalLanguage, provider);
    }

    /** Runs a raw NRQL query directly (used with manual Kiro mode, or any hand-written NRQL). */
    public LogQueryResult runNrql(String nrql) {
        return new LogQueryResult(nrql, null, nerdGraphClient.runNrql(nrql));
    }

    private LogQueryResult execute(NrqlResponse translated) {
        return new LogQueryResult(translated.nrql(), translated.explanation(),
                nerdGraphClient.runNrql(translated.nrql()));
    }
}
