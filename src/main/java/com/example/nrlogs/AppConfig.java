package com.example.nrlogs;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the translator from whatever provider ChatClients are available (may be none). */
@Configuration
public class AppConfig {

    @Bean
    public NrqlTranslator nrqlTranslator(NrLogsProperties properties,
                                         ObjectProvider<NamedChatClient> chatClients) {
        Map<LlmProvider, ChatClient> map = new EnumMap<>(LlmProvider.class);
        chatClients.orderedStream().forEach(nc -> map.putIfAbsent(nc.provider(), nc.client()));
        return new NrqlTranslator(map, properties.getLlm().getProvider(), properties);
    }
}
