CREATE TYPE merchant_status AS ENUM (
    'APPLIED', 'KYB_IN_PROGRESS', 'KYB_MANUAL_REVIEW', 'KYB_REJECTED',
    'PENDING_APPROVAL', 'ACTIVE', 'SUSPENDED', 'CLOSED'
);

CREATE TYPE kyb_status AS ENUM (
    'NOT_STARTED', 'IN_PROGRESS', 'PASSED', 'FAILED', 'MANUAL_REVIEW'
);

CREATE TYPE entity_type AS ENUM (
    'SOLE_TRADER', 'PRIVATE_LIMITED', 'PUBLIC_LIMITED', 'PARTNERSHIP',
    'LLC', 'CORPORATION', 'NON_PROFIT', 'COOPERATIVE'
);

CREATE TYPE risk_tier AS ENUM ('LOW', 'MEDIUM', 'HIGH');

CREATE TYPE rate_limit_tier AS ENUM ('STARTER', 'GROWTH', 'SCALE', 'ENTERPRISE');

CREATE TABLE merchants (
    merchant_id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_name           VARCHAR(255)    NOT NULL,
    trading_name         VARCHAR(255),
    registration_number  VARCHAR(100)    NOT NULL,
    registration_country CHAR(2)         NOT NULL,
    entity_type          entity_type     NOT NULL,
    website_url          VARCHAR(500),
    primary_currency     CHAR(3)         NOT NULL,
    status               merchant_status NOT NULL DEFAULT 'APPLIED',
    kyb_status           kyb_status      NOT NULL DEFAULT 'NOT_STARTED',
    risk_tier            risk_tier,
    rate_limit_tier      rate_limit_tier NOT NULL DEFAULT 'STARTER',
    onboarded_by         UUID,
    registered_address   JSONB           NOT NULL,
    beneficial_owners    JSONB           NOT NULL DEFAULT '[]',
    requested_corridors  JSONB           NOT NULL DEFAULT '[]',
    allowed_scopes       JSONB           NOT NULL DEFAULT '[]',
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    activated_at         TIMESTAMPTZ,
    suspended_at         TIMESTAMPTZ,
    closed_at            TIMESTAMPTZ,
    version              BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_merchants_reg_country UNIQUE (registration_number, registration_country)
);

CREATE INDEX idx_merchants_status ON merchants (status);
CREATE INDEX idx_merchants_registration_country ON merchants (registration_country);
CREATE INDEX idx_merchants_created_at ON merchants (created_at DESC);
