DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sp_user') THEN
        REVOKE UPDATE, DELETE ON merchant_audit_log FROM sp_user;
    END IF;
END
$$;
