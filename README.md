# nr-log-query

A small, embeddable Spring Boot library to query **New Relic logs using natural language**,
designed to be called from the **IntelliJ debugger** while stopped at a breakpoint.

It turns your request into NRQL, runs it against New Relic **NerdGraph**, and prints a table.

## Translation options (pick what fits — no OpenAI/Anthropic key required)

| Mode | API key? | How |
|---|---|---|
| **Manual "Kiro"** | No | `NrLogs.prompt(...)` prints a prompt → paste into Kiro → `NrLogs.nrql(...)` |
| **Ollama (local)** | No | Run Ollama locally; `NrLogs.ask(...)` works hands-free |
| OpenAI | Yes | Add the OpenAI starter + `OPENAI_API_KEY` |
| Anthropic | Yes | Add the Anthropic starter + `ANTHROPIC_API_KEY` |

New Relic access (a User API key + account id) is always required — that's where the logs are.

## Stack
- Java 21, Gradle
- Spring Boot 3.5.16 (auto-configuration library)
- Spring AI 1.1.1 (`ChatClient`) — Ollama by default; OpenAI/Anthropic optional
- New Relic NerdGraph (GraphQL) via `RestClient`

## Build & install
```bash
./gradlew build
./gradlew publishToMavenLocal   # available as com.example:nr-log-query:0.1.0
```

Add to your app (Gradle):
```groovy
implementation 'com.example:nr-log-query:0.1.0'
```

## Configure
See `src/main/resources/application-nrlogs-example.yml`. Minimum for New Relic:
```yaml
nrlogs:
  new-relic:
    api-key: ${NEW_RELIC_API_KEY}
    # account-id is optional: auto-discovered from the API key. Set it only if the
    # key can access multiple accounts (the error message will list them).
    # account-id: ${NEW_RELIC_ACCOUNT_ID}
  llm:
    provider: ollama   # ollama | openai | anthropic (used only if that provider is available)
```

> **Account id:** you usually don't need to provide it. If your key sees exactly one account,
> the tool uses it automatically. If it sees several, it fails with a message listing the ids
> and names so you can pick one for `nrlogs.new-relic.account-id`.

## Use from the IntelliJ debugger (Evaluate Expression, Alt+F8)

### Manual "Kiro" mode — no API key
```java
NrLogs.prompt("errors mentioning timeout in the payment service in the last hour")
```
This prints a ready-to-paste prompt. Copy it into the **Kiro chat**; Kiro replies with a single
NRQL query. Then run it:
```java
NrLogs.nrql("SELECT timestamp, level, message, `service.name` FROM Log WHERE ... SINCE 1 hour ago LIMIT 50")
```

### Automatic mode — local Ollama (no API key) or a cloud key
```java
NrLogs.ask("500s in the last 15 minutes");            // default provider
NrLogs.ask("db connection errors today", "ollama");   // pick a provider
NrLogs.preview("warnings from this service", "ollama"); // NRQL only, no execution
```

## Enabling Ollama (no key, local)
1. Install Ollama and pull a model: `ollama pull llama3.1`
2. Ensure it is running (`ollama serve`, default `http://localhost:11434`)
3. Set `spring.ai.ollama.chat.options.model: llama3.1` (see the example config)

The Ollama starter ships with this library, so `NrLogs.ask(...)` works once Ollama is up.

## Enabling OpenAI / Anthropic (optional)
Add the starter to **your app** (they are optional here), plus the Spring AI BOM and the key:
```groovy
implementation platform("org.springframework.ai:spring-ai-bom:1.1.1")
implementation 'org.springframework.ai:spring-ai-starter-model-openai'      // or -anthropic
```
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

## How it works
```
NrLogs.ask("natural language")
        -> NrqlTranslator (Spring AI ChatClient, structured output -> NrqlResponse)
        -> NerdGraphClient (POST /graphql, NRQL via GraphQL variables)
        -> LogQueryResult (ASCII table)

NrLogs.prompt("natural language")   // manual Kiro mode
        -> NrqlTranslator.buildManualPrompt(...)  -> paste into Kiro -> NrLogs.nrql(...)
```

## Notes
- If no automatic provider is available, `ask()` throws a clear message pointing you to
  `NrLogs.prompt(...)` (manual Kiro mode) or Ollama.
- "This service"/"this app" questions filter by `spring.application.name` (override with `nrlogs.app-name`).
