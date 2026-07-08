package com.example.nrlogs;

import org.springframework.ai.chat.client.ChatClient;

/**
 * Associates a Spring AI {@link ChatClient} with the provider it came from, so the translator
 * can offer per-request provider selection. Contributed by the optional provider auto-configs.
 */
public record NamedChatClient(LlmProvider provider, ChatClient client) {
}
