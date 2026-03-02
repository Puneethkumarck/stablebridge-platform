CREATE TYPE document_type AS ENUM (
    'CERTIFICATE_OF_INCORPORATION', 'PROOF_OF_ADDRESS', 'BENEFICIAL_OWNER_ID',
    'BANK_STATEMENT', 'AUDITED_ACCOUNTS', 'SANCTIONS_SCREENING', 'AML_POLICY'
);

CREATE TYPE document_status AS ENUM (
    'PENDING', 'UPLOADED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED'
);

CREATE TABLE merchant_documents (
    document_id   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id   UUID            NOT NULL REFERENCES merchants (merchant_id) ON DELETE CASCADE,
    kyb_id        UUID            REFERENCES kyb_verifications (kyb_id),
    document_type document_type   NOT NULL,
    status        document_status NOT NULL DEFAULT 'PENDING',
    s3_key        VARCHAR(1000),
    file_name     VARCHAR(500),
    content_type  VARCHAR(100),
    uploaded_by   UUID,
    reviewed_by   UUID,
    review_notes  TEXT,
    uploaded_at   TIMESTAMPTZ,
    reviewed_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documents_merchant_id ON merchant_documents (merchant_id);
CREATE INDEX idx_documents_kyb_id ON merchant_documents (kyb_id);
CREATE INDEX idx_documents_status ON merchant_documents (status);
