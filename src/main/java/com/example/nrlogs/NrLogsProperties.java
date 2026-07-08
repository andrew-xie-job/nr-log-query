package com.example.nrlogs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "nrlogs")
public class NrLogsProperties {

    @NestedConfigurationProperty
    private final NewRelic newRelic = new NewRelic();

    @NestedConfigurationProperty
    private final Llm llm = new Llm();

    private String defaultSince = "30 minutes ago";
    private int defaultLimit = 50;
    private String appName;

    public NewRelic getNewRelic() { return newRelic; }
    public Llm getLlm() { return llm; }
    public String getDefaultSince() { return defaultSince; }
    public void setDefaultSince(String defaultSince) { this.defaultSince = defaultSince; }
    public int getDefaultLimit() { return defaultLimit; }
    public void setDefaultLimit(int defaultLimit) { this.defaultLimit = defaultLimit; }
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public static class NewRelic {
        private String apiKey;
        private Long accountId;
        private Region region = Region.US;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }
        public Region getRegion() { return region; }
        public void setRegion(Region region) { this.region = region; }

        public String graphqlEndpoint() {
            return region == Region.EU
                    ? "https://api.eu.newrelic.com/graphql"
                    : "https://api.newrelic.com/graphql";
        }
    }

    public enum Region { US, EU }

    public static class Llm {
        /** Default automatic provider. Only used if the matching provider is actually available. */
        private LlmProvider provider = LlmProvider.OLLAMA;
        public LlmProvider getProvider() { return provider; }
        public void setProvider(LlmProvider provider) { this.provider = provider; }
    }
}
