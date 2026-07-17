## Local Setup

### Prerequisites
- Docker Desktop (WSL2 backend)
- JDK 21
- Maven (system-wide, used once to generate the wrapper) + `.\mvnw.cmd` thereafter

  ### Optional Prerequisites
- Git
- Psql client (for manual DB inspection)


#### Check Prerequisites With:
- docker --version
- wsl --status
- java -version
- mvn -version
- git --version
- psql --version


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

## Authentication Flow

- Passwords are hashed with BCrypt (strength factor 12) via Spring Security's
  `BCryptPasswordEncoder` — never stored or logged in plaintext.
- On successful login, an opaque random UUID session token is generated and stored
  in Redis as `session:{token} -> {userId}` with a 1-hour TTL. A JWT was considered
  and rejected: JWTs cannot be revoked without an additional denylist, which would
  require the same Redis lookup this design already does directly — so the opaque
  token gets instant revocation for free with strictly less complexity.
- `SessionAuthenticationFilter` (a `OncePerRequestFilter`) intercepts every request
  except `/api/auth/**` and the Swagger UI paths, extracts the `Bearer` token,
  performs a Redis `GET`, and populates `AuthenticatedUserContext` (a request-scoped
  `ThreadLocal`) on success. Missing, malformed, or expired tokens get a 401
  immediately, before the request reaches any controller.
- Registration inserts a `users` row and a `wallet_balances` row (asset_symbol=`USD`,
  a randomized amount between 1,000 and 25,000) inside a single `@Transactional`
  boundary — a failure persisting the wallet row rolls back the user row too, so no
  account can exist without its starting balance.

## Data Source Configuration

- `cryptopal.data-source=binance` (default) activates `BinancePriceProvider`, the
  sole implementation of the `PriceProvider` interface, gated via
  `@ConditionalOnProperty`. Swapping data sources later means adding a new
  `PriceProvider` implementation and flipping this one property — nothing else in
  the app changes.
- Prices come from Binance's public `GET /api/v3/ticker/price` endpoint — no API key
  or request signing required, since this is a public market-data endpoint, not a
  trading/account endpoint.
- `cryptopal.binance.symbols` (e.g. `BTCUSDT`, `ETHUSDT`) are Binance's raw pair
  names; `cryptopal.market.tracked-symbols` (e.g. `BTC`, `ETH`) are this app's
  internal symbols. `BinancePriceProvider` is the only place that translates
  between the two (by stripping the `USDT` suffix) — the rest of the app never sees
  Binance-specific naming.
- If Binance is temporarily unreachable, `BinancePriceProvider` logs the failure and
  returns its last successfully fetched prices rather than throwing, so a single
  network hiccup never breaks a scheduler tick or leaves the Redis cache empty.

  ## Trading Module

- `POST /api/trading/execute` resolves the acting user exclusively from the
  validated session (`AuthenticatedUserContext`) — a client can never specify
  which user's wallet to trade against.
- Both buy and sell lock the USD wallet row **before** the asset wallet row, in
  that fixed order, in both code paths. This consistent lock ordering prevents a
  deadlock that could otherwise occur if two concurrent trades on the same user
  (e.g. one buy, one sell) tried to acquire the same two row locks in opposite order.
- `@Transactional(isolation = REPEATABLE_READ)` wraps the whole trade; the
  pessimistic write lock on both wallet rows is the primary defense against
  concurrent double-spend, with REPEATABLE_READ as defense-in-depth.
- Domain errors (`InsufficientFundsException`, `InvalidAmountException`,
  `AssetNotFoundException`) are mapped by a shared `@RestControllerAdvice` to
  structured JSON payloads (422/400/404 respectively) — the client never sees a
  raw 500 for an expected business-rule failure.

## AI Insights Module

- `POST /api/ai/query` assembles a labeled-section prompt (`USER PORTFOLIO:`,
  `RECENT TRANSACTIONS:`, `PRICE TRENDS:`, `LATEST PRICES:`, `USER QUERY:`) from
  live data before calling Gemini: wallet balances and transaction history come
  from Postgres, while the "latest price" section is read from Redis specifically
  — never from `price_trend_log` — so the AI always answers against the same
  real-time price a trade would execute at.
- The Gemini call uses `WebClient` with a 10-second reactive `.timeout()`, an
  `.onErrorResume()` that swallows the error server-side, and a final `.block()`
  with an 11-second backstop. Blocking is a deliberate choice here, not an
  oversight: this is a Spring MVC application where the JPA calls immediately
  before and after the Gemini call already block the same servlet thread, so a
  fully reactive chain for just this one call would add complexity without
  freeing the thread for reuse elsewhere.
- On any failure — timeout, network error, invalid API key, malformed response —
  the endpoint returns `{"response": null, "error": "AI service temporarily
  unavailable"}` with a 200 status. The raw exception, stack trace, and API key
  are never included in the response body or logged to anywhere client-visible.
