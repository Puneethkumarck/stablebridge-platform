ALTER TABLE approved_corridors
    ADD CONSTRAINT uq_corridors_merchant_country
        UNIQUE (merchant_id, source_country, target_country);

ALTER TABLE approved_corridors
    ALTER COLUMN max_amount_usd TYPE NUMERIC(20, 2);
