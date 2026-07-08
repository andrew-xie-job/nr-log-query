package com.example.nrlogs;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/** Enabled only when the Anthropic starter is present and an AnthropicChatModel bean exists. */
@AutoConfiguration(before = NrLogsAutoConfiguration.class)
@ConditionalOnClass(AnthropicChatModel.class)
public class AnthropicProviderConfiguration {

    @Bean
    @ConditionalOnBean(AnthropicChatModel.class)
    public NamedChatClient anthropicNamedChatClient(AnthropicChatModel model) {
        return new NamedChatClient(LlmProvider.ANTHROPIC, ChatClient.create(model));
    }
}
