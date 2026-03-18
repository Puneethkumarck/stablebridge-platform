#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Vault initialisation script for local development (Docker Compose).
#
# Seeds the KV v2 secrets engine with per-service credentials and shared
# infrastructure secrets.  Designed to run idempotently — re-running this
# script simply overwrites existing values.
#
# Prerequisites:
#   - Vault must be running in dev mode (VAULT_DEV_ROOT_TOKEN_ID=dev-root-token)
#   - The `vault` CLI must be available (inside the Vault container)
#
# Usage:
#   docker compose exec vault /bin/sh /vault/init-vault.sh
# ---------------------------------------------------------------------------
set -euo pipefail

export VAULT_ADDR="${VAULT_ADDR:-http://127.0.0.1:8200}"
export VAULT_TOKEN="${VAULT_TOKEN:-dev-root-token}"

echo "==> Waiting for Vault to be ready..."
until vault status > /dev/null 2>&1; do
  sleep 1
done
echo "==> Vault is ready"

# ---------------------------------------------------------------------------
# Enable KV v2 secrets engine (idempotent — ignores "already mounted" error)
# ---------------------------------------------------------------------------
vault secrets enable -path=secret -version=2 kv 2>/dev/null || true
echo "==> KV v2 secrets engine enabled at secret/"

# ---------------------------------------------------------------------------
# Shared secrets (default-context = "application")
# Every service reads from secret/application as a fallback.
# ---------------------------------------------------------------------------
vault kv put secret/application \
  spring.datasource.username=sp_user \
  spring.datasource.password=sp_pass \
  spring.kafka.bootstrap-servers=localhost:9092 \
  spring.data.redis.password=""

echo "==> Shared secrets seeded at secret/application"

# ---------------------------------------------------------------------------
# S1 Payment Orchestrator
# ---------------------------------------------------------------------------
vault kv put secret/payment-orchestrator \
  spring.datasource.username=sp_user \
  spring.datasource.password=sp_pass \
  spring.datasource.url=jdbc:postgresql://localhost:5432/s1_payment_orchestrator

echo "==> Secrets seeded for payment-orchestrator"

# ---------------------------------------------------------------------------
# S2 Compliance & Travel Rule
# ---------------------------------------------------------------------------
vault kv put secret/compliance-travel-rule \
  spring.datasource.username=sp_user \
  spring.datasource.password=sp_pass \
  spring.datasource.url=jdbc:postgresql://localhost:5432/s2_compliance \
  app.compliance.worldcheck.api-key=dev-worldcheck-key \
  app.compliance.chainalysis.api-key=dev-chainalysis-key

echo "==> Secrets seeded for compliance-travel-rule"

# ---------------------------------------------------------------------------
# S3 Fiat On-Ramp
# ---------------------------------------------------------------------------
vault kv put secret/fiat-on-ramp \
  spring.datasource.username=sp_user \
  spring.datasource.password=sp_pass \
  spring.datasource.url=jdbc:postgresql://localhost:5432/s3_fiat_on_ramp \
  app.psp.stripe.api-key=sk_test_dev_vault_key \
  app.psp.stripe.webhook.webhook-secret=whsec_test_dev_vault

echo "==> Secrets seeded for fiat-on-ramp"

# ---------------------------------------------------------------------------
# S4 Blockchain & Custody
# ---------------------------------------------------------------------------
vault kv put secret/blockchain-custody \
  spring.datasource.username=dev \
  spring.datasource.password=dev \
  spring.datasource.url=jdbc:postgresql://localhost:5432/s4_blockchain_custody \
  app.custody.fireblocks.api-key=fireblocks-dev-key \
  app.custody.fireblocks.api-secret=dev-fireblocks-secret \
  app.custody.dev.evm-private-key=dev-evm-private-key

echo "==> Secrets seeded for blockchain-custody"

# ---------------------------------------------------------------------------
# S5 Fiat Off-Ramp
# ---------------------------------------------------------------------------
vault kv put secret/fiat-off-ramp \
  spring.datasource.username=sp_user \
  spring.datasource.password=sp_pass \
  spring.datasource.url=jdbc:postgresql://localhost:5432/s5_fiat_off_ramp \
  app.redemption.circle.api-key=SAND_API_KEY_VAULT \
  app.payout.modulr.webhook.webhook-secret=dev-modulr-webhook-secret

echo "==> Secrets seeded for fiat-off-ramp"

# ---------------------------------------------------------------------------
# S6 FX & Liquidity Engine
# ---------------------------------------------------------------------------
vault kv put secret/fx-liquidity-engine \
  spring.datasource.username=sp_user \
  spring.datasource.password=sp_pass \
  spring.datasource.url=jdbc:postgresql://localhost:5433/fx_rates

echo "==> Secrets seeded for fx-liquidity-engine"

# ---------------------------------------------------------------------------
# S7 Ledger & Accounting
# ---------------------------------------------------------------------------
vault kv put secret/ledger-accounting \
  spring.datasource.username=sp_user \
  spring.datasource.password=sp_pass \
  spring.datasource.url=jdbc:postgresql://localhost:5432/s7_ledger_accounting

echo "==> Secrets seeded for ledger-accounting"

# ---------------------------------------------------------------------------
# S10 API Gateway & IAM
# ---------------------------------------------------------------------------
vault kv put secret/api-gateway-iam \
  spring.datasource.username=sp_user \
  spring.datasource.password=sp_pass \
  spring.datasource.url=jdbc:postgresql://localhost:5432/s10_api_gateway_iam \
  api-gateway-iam.jwt.private-key-base64=dev-jwt-private-key-base64

echo "==> Secrets seeded for api-gateway-iam"

# ---------------------------------------------------------------------------
# S11 Merchant Onboarding
# ---------------------------------------------------------------------------
vault kv put secret/merchant-onboarding \
  spring.datasource.username=sp_user \
  spring.datasource.password=sp_pass \
  spring.datasource.url=jdbc:postgresql://localhost:5432/s11_merchant_onboarding

echo "==> Secrets seeded for merchant-onboarding"

# ---------------------------------------------------------------------------
# S13 Merchant IAM
# ---------------------------------------------------------------------------
vault kv put secret/merchant-iam \
  spring.datasource.username=sp_user \
  spring.datasource.password=sp_pass \
  spring.datasource.url=jdbc:postgresql://localhost:5432/s13_merchant_iam \
  merchant-iam.jwt.private-key-base64=dev-jwt-private-key-base64

echo "==> Secrets seeded for merchant-iam"

echo ""
echo "==> Vault initialisation complete. All service secrets seeded."
echo "==> Vault UI: http://localhost:8200  (token: dev-root-token)"
