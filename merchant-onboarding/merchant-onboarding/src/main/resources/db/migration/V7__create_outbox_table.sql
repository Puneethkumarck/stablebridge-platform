CREATE TABLE outbox_events
(
    id           UUID        NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    topic        VARCHAR(255) NOT NULL,
    event_type   VARCHAR(255) NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL             DEFAULT NOW(),
    processed    BOOLEAN      NOT NULL             DEFAULT FALSE,
    processed_at TIMESTAMPTZ,
    retry_count  INT          NOT NULL             DEFAULT 0
);

CREATE INDEX idx_outbox_events_unprocessed
    ON outbox_events (created_at)
    WHERE processed = FALSE;
