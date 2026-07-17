## Local Setup (Infrastructure - Day 1, Windows + Maven)

### Prerequisites
- Docker Desktop (WSL2 backend)
- JDK 21
- Maven (system-wide, used once to generate the wrapper) + `.\mvnw.cmd` thereafter

  ### Optional Prerequisites
- Git
- Psql client (for manual DB inspection)


#### Check Prerequisites With:
docker --version
wsl --status
java -version
mvn -version
git --version
psql --version


### Steps
1. Copy `.env.example` to `.env` and fill in real values.
2. Start infra: `docker compose --env-file .env up -d postgres redis`
3. Load env vars: `.\load-env.ps1`
4. Run the app: `.\mvnw.cmd spring-boot:run`
5. Verify: `docker exec -it cryptopal-postgres psql -U $env:POSTGRES_USER -d $env:POSTGRES_DB -c "\dt"`

### Schema Decisions
- **NUMERIC(24,8)** is used for all monetary/asset quantities instead of
  FLOAT/DOUBLE, since binary floating point cannot represent most decimal
  fractions exactly and would accumulate rounding error across repeated
  buy/sell operations on a financial ledger.
- **ON DELETE CASCADE** is applied from `wallet_balances` and `transactions`
  to `users`. This is a deliberate choice: a deleted user account should not
  leave orphaned financial records referencing a non-existent user.
- `transactions` is treated as an **immutable, append-only ledger** - the
  application layer never issues `UPDATE`/`DELETE` against this table.
- Redis is **purely a volatile cache**; no data durability is expected or
  required from it (see `docker-compose.yml` comments).

### Module Responsibilities
| Module | Responsibility |
|---|---|
| `auth` | Registration, login, JWT/session issuance |
| `market` | Price ingestion, Redis cache, `price_trend_log` writes |
| `trading` | Buy/sell execution, wallet mutation, ledger writes |
| `ai` | Gemini-backed advisory features (later day) |
| `common` | Shared entities, DTOs, exception handling |
