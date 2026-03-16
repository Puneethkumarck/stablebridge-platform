ALTER TABLE onboarding_idempotency_keys DROP CONSTRAINT onboarding_idempotency_keys_pkey;
ALTER TABLE onboarding_idempotency_keys ADD COLUMN request_method VARCHAR(10) NOT NULL DEFAULT 'POST';
ALTER TABLE onboarding_idempotency_keys ADD COLUMN request_path VARCHAR(512) NOT NULL DEFAULT '';
ALTER TABLE onboarding_idempotency_keys ADD PRIMARY KEY (idempotency_key, request_method, request_path);
ALTER TABLE onboarding_idempotency_keys ALTER COLUMN request_method DROP DEFAULT;
ALTER TABLE onboarding_idempotency_keys ALTER COLUMN request_path DROP DEFAULT;
