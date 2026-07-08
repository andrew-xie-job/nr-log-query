# NR Log Search

A small **web app** to query your New Relic logs in **plain English**. Start it, open it in a
browser, pick an account (e.g. test / prod), type a question, review/edit the generated NRQL,
and run it to get a table of matching logs.

```
"errors from the payment service in the last hour"  ->  NRQL (editable)  ->  results table
```

## UI
- A Google-style search box
- An **Account** dropdown (auto-filled from your API key: test/prod appear here)
- A **Mode** switch:
  - **English** - type a question, click **Translate**, tweak the NRQL if needed, then **Run**
  - **NRQL** - type a query directly and **Run**
- Results render as a table, with the executed NRQL shown above

## Stack
- Java 21, Spring Boot 3.5.16 (runnable web app)
- Spring AI 1.1.1 for English -> NRQL (Ollama by default; OpenAI/Anthropic optional)
- New Relic NerdGraph (GraphQL)
- Plain HTML/CSS/JS frontend in `src/main/resources/static`

## Prerequisites
- JDK 21+ (for local run) or Docker (for container run)
- A New Relic **User API key** (`NRAK-...`)
- For **English** mode: a model at runtime. No-key option = **Ollama** (local, free).
  Without a model, **NRQL** mode still works.

## Configure
Set your New Relic key (account ids are auto-discovered from it):
```bash
export NEW_RELIC_API_KEY="NRAK-xxxxxxxxxxxxxxxx"
```
Region defaults to `US` (correct for most Australian accounts). If your New Relic URL is
`one.eu.newrelic.com`, set `nrlogs.new-relic.region: EU` in `src/main/resources/application.yml`.

## Run - option A: locally with Gradle
```bash
export NEW_RELIC_API_KEY="NRAK-..."
./gradlew bootRun
# open http://localhost:8080
```
For English mode, also run Ollama:
```bash
ollama pull llama3.1
ollama serve            # http://localhost:11434
```

## Run - option B: Docker (app + Ollama together)
```bash
export NEW_RELIC_API_KEY="NRAK-..."
docker compose up --build
# one-time: pull a model into the ollama container
docker compose exec ollama ollama pull llama3.1
# open http://localhost:8080
```
Or just the app image (bring your own model / key):
```bash
docker build -t nr-log-search .
docker run -p 8080:8080 -e NEW_RELIC_API_KEY="NRAK-..." nr-log-search
```

## Using it
1. Pick an account.
2. **English mode:** type e.g. `errors in the last 15 minutes` -> **Translate**.
   The NRQL appears in an editable box -> adjust if you want -> **Run**.
3. **NRQL mode:** type e.g. `SELECT timestamp, level, message FROM Log SINCE 1 hour ago LIMIT 20` -> **Run**.

## HTTP API (used by the UI)
- `GET  /api/accounts`  -> `{ accounts: [{id,name}], translationAvailable }`
- `POST /api/translate` with `{ "text": "..." }` -> `{ nrql, explanation }`
- `POST /api/query`     with `{ "accountId": 123, "text": "...", "raw": true|false }`
  -> `{ nrql, explanation, columns, rows }`

## Accounts / test vs prod
The dropdown lists every account your API key can see (that's where test/prod appear).
To pin/relabel them, set them explicitly in `application.yml`:
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
- English mode needs a model at runtime; if none is available the UI says so and you can use
  NRQL mode. (The app can't call Kiro directly - Kiro is an interactive assistant, not a runtime API.)
- New Relic access is always required; that's where the logs live.
