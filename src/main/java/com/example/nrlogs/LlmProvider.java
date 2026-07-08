package com.example.nrlogs;

/**
 * Supported LLM providers for translating natural language into NRQL.
 */
public enum LlmProvider {
    OPENAI,
    ANTHROPIC;

    public static LlmProvider from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("LLM provider must not be blank");
        }
        String normalized = value.trim().toUpperCase();
        try {
            return LlmProvider.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unknown LLM provider '" + value + "'. Supported values: openai, anthropic");
        }
    }
}
