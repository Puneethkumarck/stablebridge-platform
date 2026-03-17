CREATE TABLE onboarding_idempotency_keys (
    idempotency_key  VARCHAR(255)   PRIMARY KEY,
    request_hash     VARCHAR(64)    NOT NULL,
    response_body    TEXT           NOT NULL DEFAULT '',
    status_code      INTEGER        NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    expires_at       TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_onboarding_idempotency_expires ON onboarding_idempotency_keys (expires_at);
