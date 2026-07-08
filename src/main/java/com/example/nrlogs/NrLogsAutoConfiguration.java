package com.example.nrlogs;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@AutoConfiguration
@EnableConfigurationProperties(NrLogsProperties.class)
public class NrLogsAutoConfiguration {

    @Bean
    public NerdGraphClient nerdGraphClient(NrLogsProperties properties) {
        return new NerdGraphClient(properties.getNewRelic());
    }

    @Bean
    public NrqlTranslator nrqlTranslator(NrLogsProperties properties,
                                         ObjectProvider<OpenAiChatModel> openAiChatModel,
                                         ObjectProvider<AnthropicChatModel> anthropicChatModel) {
        Map<LlmProvider, ChatClient> clients = new EnumMap<>(LlmProvider.class);

        OpenAiChatModel openai = openAiChatModel.getIfAvailable();
        if (openai != null) {
            clients.put(LlmProvider.OPENAI, ChatClient.create(openai));
        }
        AnthropicChatModel anthropic = anthropicChatModel.getIfAvailable();
        if (anthropic != null) {
            clients.put(LlmProvider.ANTHROPIC, ChatClient.create(anthropic));
        }

        return new NrqlTranslator(clients, properties.getLlm().getProvider(), properties);
    }

    @Bean
    public NrLogQueryService nrLogQueryService(NrqlTranslator translator,
                                               NerdGraphClient nerdGraphClient,
                                               NrLogsProperties properties,
                                               Environment environment) {
        if (!StringUtils.hasText(properties.getAppName())) {
            String appName = environment.getProperty("spring.application.name");
            if (StringUtils.hasText(appName)) {
                properties.setAppName(appName);
            }
        }
        NrLogQueryService service = new NrLogQueryService(translator, nerdGraphClient);
        NrLogs.bind(service);
        return service;
    }
}
