package com.example.nrlogs;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Enabled when the Ollama starter is present and an OllamaChatModel bean exists.
 * This is the recommended no-API-key automatic provider (runs a local model at localhost:11434).
 */
@AutoConfiguration(before = NrLogsAutoConfiguration.class)
@ConditionalOnClass(OllamaChatModel.class)
public class OllamaProviderConfiguration {

    @Bean
    @ConditionalOnBean(OllamaChatModel.class)
    public NamedChatClient ollamaNamedChatClient(OllamaChatModel model) {
        return new NamedChatClient(LlmProvider.OLLAMA, ChatClient.create(model));
    }
}
