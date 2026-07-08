package com.example.nrlogs;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(OpenAiChatModel.class)
public class OpenAiProviderConfiguration {
    @Bean
    @ConditionalOnBean(OpenAiChatModel.class)
    public NamedChatClient openAiNamedChatClient(OpenAiChatModel model) {
        return new NamedChatClient(LlmProvider.OPENAI, ChatClient.create(model));
    }
}
