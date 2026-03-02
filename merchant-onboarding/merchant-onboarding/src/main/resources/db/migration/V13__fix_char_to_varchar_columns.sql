-- Change CHAR(n) to VARCHAR(n) to align with JPA/Hibernate entity mapping
-- Hibernate maps String fields as VARCHAR, but CHAR is stored as bpchar in PostgreSQL

-- merchants table
ALTER TABLE merchants ALTER COLUMN registration_country TYPE VARCHAR(2);
ALTER TABLE merchants ALTER COLUMN primary_currency TYPE VARCHAR(3);

-- approved_corridors table
ALTER TABLE approved_corridors ALTER COLUMN source_country TYPE VARCHAR(2);
ALTER TABLE approved_corridors ALTER COLUMN target_country TYPE VARCHAR(2);
