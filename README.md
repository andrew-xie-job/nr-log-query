# nr-log-query

A small, embeddable Spring Boot library to query **New Relic logs using natural language**,
designed to be called from the **IntelliJ debugger** while stopped at a breakpoint.

```
NrLogs.ask("show errors from the checkout service in the last 10 minutes")
```

Translates the request into NRQL via **Spring AI** (OpenAI or Anthropic), runs it against
New Relic **NerdGraph**, and prints a table.

## Stack
- Java 21, Gradle
- Spring Boot 3.5.16 (auto-configuration library)
- Spring AI 1.1.1 (`ChatClient`, OpenAI + Anthropic)
- New Relic NerdGraph (GraphQL) via `RestClient`

## Build & install
```bash
./gradlew build
./gradlew publishToMavenLocal   # available as com.example:nr-log-query:0.1.0
```
> Note: if there is no Gradle wrapper yet, run `gradle wrapper` first, or import into IntelliJ.

## Add to your app (Gradle)
```groovy
implementation 'com.example:nr-log-query:0.1.0'
```

## Configure
See `src/main/resources/application-nrlogs-example.yml`. Minimum:
```yaml
nrlogs:
  new-relic:
    api-key: ${NEW_RELIC_API_KEY}
    account-id: ${NEW_RELIC_ACCOUNT_ID}
  llm:
    provider: openai
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
```

## Use from the IntelliJ debugger
Pause at a breakpoint, open **Evaluate Expression** (Alt+F8), run:
```java
NrLogs.ask("errors mentioning timeout in the payment service in the last hour");
NrLogs.ask("500s in the last 15 minutes", "anthropic");   // pick a provider
NrLogs.preview("db errors since yesterday", "openai");     // NRQL only, no execution
NrLogs.nrql("SELECT timestamp, message FROM Log WHERE level = 'ERROR' SINCE 1 hour ago LIMIT 20");
```
Or inject `NrLogQueryService` anywhere in your app.
