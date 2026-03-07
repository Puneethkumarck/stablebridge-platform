-- ============================================================
-- S6 FX & Liquidity Engine — Initial Schema
-- Database: TimescaleDB (PostgreSQL extension, port 5433)
-- ============================================================

-- Guard: create roles only if they do not exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'fx_app') THEN
        CREATE ROLE fx_app LOGIN;
    END IF;
END $$;

-- ============================================================
-- fx_quotes
-- ============================================================
CREATE TABLE fx_quotes (
    quote_id        UUID            NOT NULL DEFAULT gen_random_uuid(),
    from_currency   VARCHAR(3)      NOT NULL,
    to_currency     VARCHAR(3)      NOT NULL,
    source_amount   NUMERIC(20, 8)  NOT NULL,
    target_amount   NUMERIC(20, 8)  NOT NULL,
    rate            NUMERIC(20, 10) NOT NULL CHECK (rate > 0),
    inverse_rate    NUMERIC(20, 10) NOT NULL,
    fee_bps         INT             NOT NULL DEFAULT 0,
    fee_amount      NUMERIC(20, 8)  NOT NULL DEFAULT 0,
    provider        VARCHAR(100)    NOT NULL,
    provider_ref    VARCHAR(200)    NULL,
    status          VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ     NOT NULL,
    CONSTRAINT fx_quotes_pkey PRIMARY KEY (quote_id),
    CONSTRAINT fx_quotes_status_check CHECK (status IN ('ACTIVE','LOCKED','EXPIRED'))
);

CREATE INDEX fx_quotes_currencies_idx ON fx_quotes (from_currency, to_currency, created_at DESC);
CREATE INDEX fx_quotes_expires_idx ON fx_quotes (expires_at) WHERE status = 'ACTIVE';

-- ============================================================
-- fx_rate_locks
-- ============================================================
CREATE TABLE fx_rate_locks (
    lock_id         UUID            NOT NULL DEFAULT gen_random_uuid(),
    quote_id        UUID            NOT NULL REFERENCES fx_quotes(quote_id),
    payment_id      UUID            NOT NULL,
    correlation_id  UUID            NOT NULL,
    from_currency   VARCHAR(3)      NOT NULL,
    to_currency     VARCHAR(3)      NOT NULL,
    source_amount   NUMERIC(20, 8)  NOT NULL,
    target_amount   NUMERIC(20, 8)  NOT NULL,
    locked_rate     NUMERIC(20, 10) NOT NULL CHECK (locked_rate > 0),
    fee_bps         INT             NOT NULL,
    fee_amount      NUMERIC(20, 8)  NOT NULL,
    source_country  VARCHAR(2)      NOT NULL,
    target_country  VARCHAR(2)      NOT NULL,
    status          VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    locked_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ     NOT NULL,
    consumed_at     TIMESTAMPTZ     NULL,
    CONSTRAINT fx_rate_locks_pkey PRIMARY KEY (lock_id),
    CONSTRAINT fx_rate_locks_payment_id_unique UNIQUE (payment_id),
    CONSTRAINT fx_rate_locks_status_check CHECK (status IN ('ACTIVE','CONSUMED','EXPIRED'))
);

CREATE INDEX fx_rate_locks_payment_id_idx ON fx_rate_locks (payment_id);
CREATE INDEX fx_rate_locks_expires_idx ON fx_rate_locks (expires_at) WHERE status = 'ACTIVE';

-- ============================================================
-- rate_history  (TimescaleDB hypertable)
-- ============================================================
CREATE TABLE rate_history (
    id              BIGSERIAL,
    from_currency   VARCHAR(3)      NOT NULL,
    to_currency     VARCHAR(3)      NOT NULL,
    rate            NUMERIC(20, 10) NOT NULL,
    bid             NUMERIC(20, 10) NULL,
    ask             NUMERIC(20, 10) NULL,
    provider        VARCHAR(100)    NOT NULL,
    source_type     VARCHAR(20)     NOT NULL,
    recorded_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT rate_history_pkey PRIMARY KEY (id, recorded_at)
);

-- Convert to TimescaleDB hypertable only if the extension is available
-- (TestContainers uses plain PostgreSQL where TimescaleDB is not present)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN
        PERFORM create_hypertable('rate_history', 'recorded_at', chunk_time_interval => INTERVAL '1 hour');
    END IF;
END
$$;

CREATE INDEX rate_history_currencies_time_idx
    ON rate_history (from_currency, to_currency, recorded_at DESC);

-- ============================================================
-- liquidity_pools
-- ============================================================
CREATE TABLE liquidity_pools (
    pool_id             UUID            NOT NULL DEFAULT gen_random_uuid(),
    from_currency       VARCHAR(3)      NOT NULL,
    to_currency         VARCHAR(3)      NOT NULL,
    available_balance   NUMERIC(20, 8)  NOT NULL DEFAULT 0 CHECK (available_balance >= 0),
    reserved_balance    NUMERIC(20, 8)  NOT NULL DEFAULT 0 CHECK (reserved_balance >= 0),
    minimum_threshold   NUMERIC(20, 8)  NOT NULL,
    maximum_capacity    NUMERIC(20, 8)  NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT liquidity_pools_pkey PRIMARY KEY (pool_id),
    CONSTRAINT liquidity_pools_corridor_unique UNIQUE (from_currency, to_currency)
);

-- ============================================================
-- hedging_positions (POST-MVP -- Phase 2b/Phase 6)
-- ============================================================
CREATE TABLE hedging_positions (
    position_id     UUID            NOT NULL DEFAULT gen_random_uuid(),
    source_country  VARCHAR(2)      NOT NULL,
    target_country  VARCHAR(2)      NOT NULL,
    from_currency   VARCHAR(3)      NOT NULL,
    to_currency     VARCHAR(3)      NOT NULL,
    notional        NUMERIC(20, 8)  NOT NULL,
    direction       VARCHAR(5)      NOT NULL,
    provider        VARCHAR(100)    NOT NULL,
    open_rate       NUMERIC(20, 10) NOT NULL,
    current_rate    NUMERIC(20, 10) NULL,
    pnl             NUMERIC(20, 8)  NULL,
    status          VARCHAR(10)     NOT NULL DEFAULT 'OPEN',
    opened_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    closed_at       TIMESTAMPTZ     NULL,
    CONSTRAINT hedging_positions_pkey PRIMARY KEY (position_id),
    CONSTRAINT hedging_direction_check CHECK (direction IN ('LONG','SHORT')),
    CONSTRAINT hedging_status_check CHECK (status IN ('OPEN','CLOSED','EXPIRED'))
);

CREATE INDEX hedging_open_idx ON hedging_positions (status, from_currency, to_currency)
    WHERE status = 'OPEN';

-- ============================================================
-- fx_outbox_events (Namastack outbox -- service-specific prefix)
-- ============================================================
CREATE TABLE fx_outbox_events (
    id              VARCHAR(255)    NOT NULL,
    aggregate_id    VARCHAR(255)    NOT NULL,
    event_type      VARCHAR(100)    NOT NULL,
    topic           VARCHAR(200)    NOT NULL,
    partition_key   VARCHAR(128)    NOT NULL,
    payload         JSONB           NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT fx_outbox_events_pkey PRIMARY KEY (id)
);
