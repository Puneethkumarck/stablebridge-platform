CREATE TABLE kyb_verifications (
    kyb_id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id   UUID        NOT NULL REFERENCES merchants (merchant_id) ON DELETE CASCADE,
    provider      VARCHAR(50) NOT NULL,
    provider_ref  VARCHAR(255),
    status        kyb_status  NOT NULL DEFAULT 'IN_PROGRESS',
    risk_signals  JSONB       NOT NULL DEFAULT '{}',
    reviewed_by   UUID,
    review_notes  TEXT,
    initiated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at  TIMESTAMPTZ
);

CREATE INDEX idx_kyb_merchant_id ON kyb_verifications (merchant_id);
CREATE INDEX idx_kyb_status ON kyb_verifications (status);
CREATE INDEX idx_kyb_provider_ref ON kyb_verifications (provider_ref);
