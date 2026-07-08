package com.example.nrlogs;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(OllamaChatModel.class)
public class OllamaProviderConfiguration {
    @Bean
    @ConditionalOnBean(OllamaChatModel.class)
    public NamedChatClient ollamaNamedChatClient(OllamaChatModel model) {
        return new NamedChatClient(LlmProvider.OLLAMA, ChatClient.create(model));
    }
}
