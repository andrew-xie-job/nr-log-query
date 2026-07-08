package com.example.nrlogs;

/**
 * Structured output returned by the LLM when translating natural language into NRQL.
 */
public record NrqlResponse(String nrql, String explanation) {
}
