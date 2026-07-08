package com.example.nrlogs;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

/** Orchestrates account listing, English->NRQL translation, and NRQL execution. */
@Service
public class LogQueryService {

    private final NrqlTranslator translator;
    private final NerdGraphClient nerdGraph;

    public LogQueryService(NrqlTranslator translator, NerdGraphClient nerdGraph) {
        this.translator = translator;
        this.nerdGraph = nerdGraph;
    }

    public List<AccountInfo> accounts() {
        return nerdGraph.listAccounts();
    }

    public boolean translationAvailable() {
        return translator.hasProvider();
    }

    /** Translate English to NRQL without running it (for the preview/edit step). */
    public NrqlResponse translateOnly(String question) {
        return translator.translate(question);
    }

    /** Plain-English query: translate then run. */
    public QueryResult ask(long accountId, String question) {
        NrqlResponse t = translator.translate(question);
        List<Map<String, Object>> rows = nerdGraph.runNrql(accountId, t.nrql());
        return QueryResult.of(t.nrql(), t.explanation(), rows);
    }

    /** Raw NRQL query: run as-is. */
    public QueryResult runNrql(long accountId, String nrql) {
        return QueryResult.of(nrql, null, nerdGraph.runNrql(accountId, nrql));
    }
}
