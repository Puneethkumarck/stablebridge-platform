-- V8: Add optimistic locking version column to merchant_users

ALTER TABLE merchant_users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
