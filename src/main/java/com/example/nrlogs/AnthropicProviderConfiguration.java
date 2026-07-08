package com.example.nrlogs;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(AnthropicChatModel.class)
public class AnthropicProviderConfiguration {
    @Bean
    @ConditionalOnBean(AnthropicChatModel.class)
    public NamedChatClient anthropicNamedChatClient(AnthropicChatModel model) {
        return new NamedChatClient(LlmProvider.ANTHROPIC, ChatClient.create(model));
    }
}
