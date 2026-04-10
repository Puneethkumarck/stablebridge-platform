-- ============================================================
-- V6: Seed USD-GBP liquidity pool for US->GB corridor
-- ============================================================

INSERT INTO liquidity_pools (pool_id, from_currency, to_currency,
                             available_balance, reserved_balance,
                             minimum_threshold, maximum_capacity,
                             updated_at, version)
VALUES (gen_random_uuid(), 'USD', 'GBP',
        1000000.00000000, 0.00000000,
        100000.00000000, 5000000.00000000,
        now(), 0)
ON CONFLICT (from_currency, to_currency) DO NOTHING;
