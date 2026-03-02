ALTER TABLE merchant_contacts
    ADD COLUMN email_hash VARCHAR(64);

CREATE INDEX idx_contacts_email_hash
    ON merchant_contacts (email_hash);
