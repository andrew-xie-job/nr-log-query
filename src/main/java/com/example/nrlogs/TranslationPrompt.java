package com.example.nrlogs;

/**
 * A ready-to-paste prompt for manual translation via the Kiro chat (no API key required).
 *
 * <p>Produced by {@link NrLogs#prompt(String)}. Its {@link #toString()} prints step-by-step
 * instructions plus the prompt, so it reads well in IntelliJ's Evaluate Expression window.</p>
 */
public record TranslationPrompt(String promptText) {

    @Override
    public String toString() {
        String nl = System.lineSeparator();
        return nl
                + "==== Manual (Kiro) translation - no API key needed ====" + nl
                + "1) Copy the PROMPT below into the Kiro chat." + nl
                + "2) Kiro replies with a single NRQL query." + nl
                + "3) Run:  NrLogs.nrql(\"<paste the NRQL Kiro returned>\")" + nl
                + "-------------------------- PROMPT --------------------------" + nl
                + promptText + nl
                + "------------------------------------------------------------";
    }
}
