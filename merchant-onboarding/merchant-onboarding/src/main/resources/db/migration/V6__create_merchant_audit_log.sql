CREATE TABLE merchant_audit_log (
    log_id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id   UUID        NOT NULL,
    actor_id      UUID,
    action        VARCHAR(100) NOT NULL,
    previous_state JSONB,
    new_state     JSONB,
    metadata      JSONB       NOT NULL DEFAULT '{}',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_merchant_id ON merchant_audit_log (merchant_id);
CREATE INDEX idx_audit_created_at ON merchant_audit_log (created_at DESC);
CREATE INDEX idx_audit_action ON merchant_audit_log (action);
