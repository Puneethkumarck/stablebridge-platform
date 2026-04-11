-- ============================================================
-- V4: Widen recipient_account_hash for real SHA-256 digest
-- sha256: prefix (7 chars) + 64 hex chars = 71 chars
-- ============================================================

ALTER TABLE payout_orders ALTER COLUMN recipient_account_hash TYPE VARCHAR(128);
