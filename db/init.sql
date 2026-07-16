-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- users
CREATE TABLE IF NOT EXISTS users (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(64) UNIQUE NOT NULL,
    email VARCHAR(128) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- wallet_balances
CREATE TABLE IF NOT EXISTS wallet_balances (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    asset_symbol VARCHAR(16) NOT NULL,
    amount NUMERIC(24, 8) NOT NULL DEFAULT 0 CHECK (amount >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (user_id, asset_symbol)
    );

-- transactions
CREATE TABLE IF NOT EXISTS transactions (
                                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    asset_symbol VARCHAR(16) NOT NULL,
    transaction_type VARCHAR(8) NOT NULL CHECK (transaction_type IN ('BUY', 'SELL')),
    volume NUMERIC(24, 8) NOT NULL CHECK (volume > 0),
    execution_price NUMERIC(24, 8) NOT NULL,
    executed_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- price_trend_log
CREATE TABLE IF NOT EXISTS price_trend_log (
                                               id BIGSERIAL PRIMARY KEY,
                                               asset_symbol VARCHAR(16) NOT NULL,
    price NUMERIC(24, 8) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- Index creation is already safe if tables are clean, but these are fine
CREATE INDEX IF NOT EXISTS idx_price_trend_symbol_time ON price_trend_log (asset_symbol, recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_user_time ON transactions (user_id, executed_at DESC);