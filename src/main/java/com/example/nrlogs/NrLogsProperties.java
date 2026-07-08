package com.example.nrlogs;

import java.util.ArrayList;
import java.util.List;

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

    public NewRelic getNewRelic() { return newRelic; }
    public Llm getLlm() { return llm; }
    public String getDefaultSince() { return defaultSince; }
    public void setDefaultSince(String defaultSince) { this.defaultSince = defaultSince; }
    public int getDefaultLimit() { return defaultLimit; }
    public void setDefaultLimit(int defaultLimit) { this.defaultLimit = defaultLimit; }

    public static class NewRelic {
        private String apiKey;
        private Region region = Region.US;
        /** Optional named accounts for the dropdown. If empty, accounts are auto-discovered. */
        private List<Account> accounts = new ArrayList<>();

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public Region getRegion() { return region; }
        public void setRegion(Region region) { this.region = region; }
        public List<Account> getAccounts() { return accounts; }
        public void setAccounts(List<Account> accounts) { this.accounts = accounts; }

        public String graphqlEndpoint() {
            return region == Region.EU
                    ? "https://api.eu.newrelic.com/graphql"
                    : "https://api.newrelic.com/graphql";
        }
    }

    public static class Account {
        private String name;
        private Long id;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    public enum Region { US, EU }

    public static class Llm {
        private LlmProvider provider = LlmProvider.OLLAMA;
        public LlmProvider getProvider() { return provider; }
        public void setProvider(LlmProvider provider) { this.provider = provider; }
    }
}
