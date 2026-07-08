package com.example.nrlogs;

public class NrLogQueryService {

    private final NrqlTranslator translator;
    private final NerdGraphClient nerdGraphClient;

    public NrLogQueryService(NrqlTranslator translator, NerdGraphClient nerdGraphClient) {
        this.translator = translator;
        this.nerdGraphClient = nerdGraphClient;
    }

    public LogQueryResult ask(String naturalLanguage) {
        NrqlResponse translated = translator.translate(naturalLanguage);
        return execute(translated);
    }

    public LogQueryResult ask(String naturalLanguage, LlmProvider provider) {
        NrqlResponse translated = translator.translate(naturalLanguage, provider);
        return execute(translated);
    }

    public NrqlResponse preview(String naturalLanguage, LlmProvider provider) {
        return translator.translate(naturalLanguage, provider);
    }

    public LogQueryResult runNrql(String nrql) {
        return new LogQueryResult(nrql, null, nerdGraphClient.runNrql(nrql));
    }

    private LogQueryResult execute(NrqlResponse translated) {
        return new LogQueryResult(
                translated.nrql(),
                translated.explanation(),
                nerdGraphClient.runNrql(translated.nrql()));
    }
}
