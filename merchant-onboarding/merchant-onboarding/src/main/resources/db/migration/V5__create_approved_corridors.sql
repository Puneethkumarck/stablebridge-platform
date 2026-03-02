CREATE TABLE approved_corridors (
    corridor_id     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id     UUID        NOT NULL REFERENCES merchants (merchant_id) ON DELETE CASCADE,
    source_country  CHAR(2)     NOT NULL,
    target_country  CHAR(2)     NOT NULL,
    currencies      JSONB       NOT NULL DEFAULT '[]',
    max_amount_usd  NUMERIC(20, 6) NOT NULL,
    approved_by     UUID        NOT NULL,
    approved_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_corridors_merchant_id ON approved_corridors (merchant_id);
CREATE INDEX idx_corridors_source_target ON approved_corridors (source_country, target_country);
CREATE INDEX idx_corridors_active_expiry ON approved_corridors (is_active, expires_at)
    WHERE is_active = TRUE;
