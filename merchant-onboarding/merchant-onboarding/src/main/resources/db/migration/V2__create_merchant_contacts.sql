CREATE TYPE contact_role AS ENUM (
    'PRIMARY', 'TECHNICAL', 'FINANCE', 'COMPLIANCE', 'LEGAL'
);

CREATE TABLE merchant_contacts (
    contact_id   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id  UUID         NOT NULL REFERENCES merchants (merchant_id) ON DELETE CASCADE,
    role         contact_role NOT NULL,
    full_name    VARCHAR(255) NOT NULL,
    email        VARCHAR(320) NOT NULL,
    phone        VARCHAR(50),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contacts_merchant_id ON merchant_contacts (merchant_id);
CREATE INDEX idx_contacts_email ON merchant_contacts (email);
