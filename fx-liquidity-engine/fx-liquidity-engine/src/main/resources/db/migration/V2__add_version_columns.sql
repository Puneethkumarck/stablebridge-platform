-- ============================================================
-- V2: Add version columns for optimistic locking (JPA @Version)
-- ============================================================

ALTER TABLE fx_quotes ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE fx_rate_locks ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE liquidity_pools ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
