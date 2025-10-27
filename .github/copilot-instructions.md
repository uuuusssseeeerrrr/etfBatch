<!-- Copilot / AI agent instructions for etfBatch -->

Purpose
-------
This file tells an AI coding agent how this repository is structured and what to do first to be productive. Focus on concrete, discoverable patterns: build/run/test commands, where secrets/config come from, main entrypoints, DI & routing, and external integrations.

Quick commands (Windows PowerShell)
----------------------------------
- Build fat JAR:  `.
  gradlew shadowJar`  (produces `build/libs/etfBatch.jar`)
- Run packaged app: `java -jar build/libs/etfBatch.jar`
- Run directly via Gradle: `.
  gradlew run` (uses `application.mainClass`)
- Run tests: `.
  gradlew test`

Key files / entrypoints
-----------------------
- `src/main/kotlin/com/ietf/etfbatch/EtfBatchApplication.kt` — Application entry; Ktor module() is mounted here.
- `src/main/resources/application.conf` — Ktor module registration and port (7777).
- `src/main/kotlin/com/ietf/etfbatch/BatchApiController.kt` — Primary HTTP routes (see list below).
- `build.gradle.kts` — Gradle Kotlin DSL; sets `mainClass = com.ietf.etfbatch.EtfBatchApplicationKt` and a `shadowJar` task.

Important runtime behavior & services
-----------------------------------
- Routing: All batch endpoints are under `authenticate("tokenAuth")` in `BatchApiController.kt`. Endpoints:
  - POST /prices/etf
  - POST /prices/stock
  - POST /stock-infos
  - POST /histories/cleanup
  - POST /rate
  - POST /etf-stocks/sync
  - POST /stock-list/sync
- Authentication: `SecurityConfig.kt` uses Vault to read a `batchtoken` and compares bearer token header to that value.
- DI: `KoinConfig.kt` registers services with `singleOf(...)`. Named `ProcessData` implementations are provided with qualifiers: `amova`, `asset`, `globalx`, `mitsubishi`, `nomura`, `simplex`.
- DB: `DataSourceFactory.init()` (in `DatabaseConfig.kt`) connects via the MariaDB JDBC driver and configures Exposed's TransactionManager.

Secrets & external integrations
--------------------------------
- Vault: `src/main/kotlin/com/ietf/etfbatch/config/VaultConfig.kt` uses the Oracle Cloud Infrastructure SDK and reads the local OCI config via `ConfigFileReader.parseDefault()` (~/.oci/config). Secrets used in code (examples):
  - `db_url`, `db_url_docker`, `db_password` — used by `DataSourceFactory`
  - `batchtoken` — used by `SecurityConfig`
  - `kis_key`, `kis_secret` — used by `HttpClientConfig` for KIS API
- DOCKER_ENV: set environment variable `DOCKER_ENV=true` to switch `DataSourceFactory` to use `db_url_docker`.

HTTP client & external APIs
--------------------------
- `HttpClientConfig.kt` creates a Ktor `HttpClient` with default base URL `https://openapi.koreainvestment.com:9443` and automatically adds `appkey`/`appsecret` headers (from Vault) to non-/token requests.

Testing & logging
-----------------
- Tests: standard Gradle test task (`.
  gradlew test`) — project uses `ktor.server.test.host` and `kotlin.test`.
- Logging: `src/main/resources/logback.xml`. Note Exposed SQL logger is set to DEBUG — useful when inspecting DB queries.

Conventions & patterns to follow
--------------------------------
- Keep business logic in service classes under `etf/`, `rate/`, `stock/`, `table/` directories; controllers (routing) are thin and call injected services.
- Use Koin for DI; prefer adding services to `KoinConfig.kt` via `singleOf` or `single<Interface>(named("qualifier"))` for `ProcessData` implementations.
- Secrets are never in code — they come from OCI Vault via `VaultConfig`. When writing code that needs external credentials, follow the same pattern.
- Error handling pattern in routes: exceptions are logged and a simple text response is returned (see `BatchApiController.kt`). Follow that approach for non-fatal batch failures.

Where to look first when editing
--------------------------------
- Add new endpoints: `BatchApiController.kt` (routing) + service in the appropriate package + register in `KoinConfig.kt`.
- If code needs configuration/secrets: add key usage in `VaultConfig` and reference the key name; prefer reading via `VaultConfig.getVaultSecret("key")`.
- When troubleshooting DB issues: enable DEBUG for `org.jetbrains.exposed.sql.Slf4jSqlLogger` in `logback.xml` (already enabled here).

Notes & caveats
---------------
- `VaultConfig` hardcodes an OCI vault endpoint. Be careful when editing — this may be environment-specific.
- The app defaults to port 7777 (see `application.conf`).
- The code expects OCI credentials (`~/.oci/config`) and Vault secret IDs to be present; local development requires these secrets to be available.

If anything here is unclear, tell me which area you want expanded (build, secrets, DI, routes, or a walkthrough for adding a new batch job).
