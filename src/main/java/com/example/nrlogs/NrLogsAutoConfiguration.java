package com.example.nrlogs;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Core auto-configuration. Has no compile-time dependency on any specific LLM provider:
 * automatic providers are contributed as {@link NamedChatClient} beans by the optional
 * provider configurations. If none are present, manual "Kiro" mode still works.
 */
@AutoConfiguration
@EnableConfigurationProperties(NrLogsProperties.class)
public class NrLogsAutoConfiguration {

    @Bean
    public NerdGraphClient nerdGraphClient(NrLogsProperties properties) {
        return new NerdGraphClient(properties.getNewRelic());
    }

    @Bean
    public NrqlTranslator nrqlTranslator(NrLogsProperties properties,
                                         ObjectProvider<NamedChatClient> chatClients) {
        Map<LlmProvider, ChatClient> map = new EnumMap<>(LlmProvider.class);
        chatClients.orderedStream().forEach(nc -> map.putIfAbsent(nc.provider(), nc.client()));
        return new NrqlTranslator(map, properties.getLlm().getProvider(), properties);
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
