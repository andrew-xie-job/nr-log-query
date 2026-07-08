package com.example.nrlogs;

/**
 * Supported automatic LLM providers for translating natural language into NRQL.
 * (Manual "Kiro" mode needs no provider and is always available.)
 */
public enum LlmProvider {
    OPENAI,
    ANTHROPIC,
    OLLAMA;

    public static LlmProvider from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("LLM provider must not be blank");
        }
        try {
            return LlmProvider.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unknown LLM provider '" + value + "'. Supported values: openai, anthropic, ollama");
        }
    }
}
