# NR Log Search

A small **web app** to query your New Relic logs in **plain English**. Start it, open it in a
browser, pick an account (e.g. test / prod), type a question, and get a table of matching logs.

```
"errors from the payment service in the last hour"  ->  NRQL  ->  results table
```

## What it looks like
- A Google-style search box
- An **Account** dropdown (auto-populated from your API key: your test/prod accounts appear here)
- A **Mode** switch: **English** (LLM translates to NRQL) or **NRQL** (type a query directly)
- Results rendered as a table, with the executed NRQL shown above it

## Stack
- Java 21, Spring Boot 3.5.16 (web app, not a library)
- Spring AI 1.1.1 for English -> NRQL (Ollama by default; OpenAI/Anthropic optional)
- New Relic NerdGraph (GraphQL)
- Plain HTML/CSS/JS frontend served from `src/main/resources/static`

## Prerequisites
- JDK 21+
- A New Relic **User API key** (`NRAK-...`)
- For **English** mode: a running model. The no-key option is **Ollama** (local, free).
  Without a model you can still use **NRQL** mode.

## 1. Configure
Set your New Relic key (account ids are auto-discovered from it):
```bash
export NEW_RELIC_API_KEY="NRAK-xxxxxxxxxxxxxxxx"
```
Region defaults to `US` (correct for most Australian accounts). If your New Relic URL is
`one.eu.newrelic.com`, set `nrlogs.new-relic.region: EU` in `src/main/resources/application.yml`.

## 2. (Optional) Set up Ollama for English mode - no API key
```bash
# install Ollama from https://ollama.com , then:
ollama pull llama3.1
ollama serve            # serves http://localhost:11434
```
The app is preconfigured to use `llama3.1` at `localhost:11434`.

To use OpenAI/Anthropic instead: in `build.gradle` change the matching
`compileOnly` line to `implementation`, then set the key under `spring.ai.*` in `application.yml`.

## 3. Run
```bash
export NEW_RELIC_API_KEY="NRAK-..."
./gradlew bootRun
```
Open **http://localhost:8080**.

- Pick an account, keep Mode = **English**, type: `errors in the last 15 minutes`, hit Search.
- No model yet? Switch Mode to **NRQL** and type:
  `SELECT timestamp, level, message FROM Log SINCE 1 hour ago LIMIT 20`

Build a runnable jar instead:
```bash
./gradlew bootJar
java -jar build/libs/nr-log-query-0.1.0.jar
```

## HTTP API (used by the UI)
- `GET  /api/accounts` -> `{ accounts: [{id, name}], translationAvailable: boolean }`
- `POST /api/query` with `{ "accountId": 123, "text": "...", "raw": false }`
  -> `{ nrql, explanation, columns, rows }`  (set `raw: true` to run NRQL directly)

## Accounts / test vs prod
The dropdown is filled from `GET /api/accounts`, which lists every account your API key can see -
that's where your **test** and **prod** accounts show up. To pin/relabel them, set them explicitly
in `application.yml`:
```yaml
nrlogs:
  new-relic:
    accounts:
      - name: test
        id: 1111111
      - name: prod
        id: 2222222
```

## Notes
- English mode needs a model at runtime; if none is available the app tells you and you can use
  NRQL mode. (This app can't call Kiro directly - Kiro is an interactive assistant, not a runtime API.)
- New Relic access is always required; that's where the logs live.
