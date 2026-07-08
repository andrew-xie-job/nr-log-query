package com.example.nrlogs;

/**
 * Static entry point designed for use from the IntelliJ debugger.
 * While paused at a breakpoint, open Evaluate Expression (Alt+F8) and run:
 *   NrLogs.ask("show errors from the checkout service in the last 10 minutes")
 */
public final class NrLogs {

    private static volatile NrLogQueryService service;

    private NrLogs() {
    }

    static void bind(NrLogQueryService instance) {
        service = instance;
    }

    public static LogQueryResult ask(String naturalLanguage) {
        return require().ask(naturalLanguage);
    }

    public static LogQueryResult ask(String naturalLanguage, String provider) {
        return require().ask(naturalLanguage, LlmProvider.from(provider));
    }

    public static NrqlResponse preview(String naturalLanguage, String provider) {
        return require().preview(naturalLanguage, LlmProvider.from(provider));
    }

    public static LogQueryResult nrql(String nrql) {
        return require().runNrql(nrql);
    }

    private static NrLogQueryService require() {
        NrLogQueryService current = service;
        if (current == null) {
            throw new IllegalStateException(
                    "NrLogs is not initialized yet. It is populated once the Spring context has "
                            + "started and the nr-log-query auto-configuration has run.");
        }
        return current;
    }
}
