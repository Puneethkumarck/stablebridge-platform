#!/bin/bash
# Phase 3 E2E Integration Tests — Full Payment Sandwich
#
# Builds all 7 service images, starts the Docker Compose stack,
# runs the Phase 3 integration tests, and tears down the stack.
#
# Requirements: Docker (OrbStack/Docker Desktop) with >= 12 GB memory
# Services: S1, S2, S3, S4, S5, S6, S7 + Postgres, TimescaleDB, Redis, Redpanda, Temporal, WireMock

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.phase3-test.yml"

echo "═══════════════════════════════════════════════════════════"
echo "  Phase 3 E2E Integration Tests — Full Payment Sandwich"
echo "═══════════════════════════════════════════════════════════"
echo ""

# Step 1: Build all service Docker images
echo "▶ Step 1: Building service Docker images..."
cd "$PROJECT_ROOT"
./gradlew \
  :payment-orchestrator:payment-orchestrator:jibDockerBuild \
  :compliance-travel-rule:compliance-travel-rule:jibDockerBuild \
  :fx-liquidity-engine:fx-liquidity-engine:jibDockerBuild \
  :fiat-on-ramp:fiat-on-ramp:jibDockerBuild \
  :blockchain-custody:blockchain-custody:jibDockerBuild \
  :fiat-off-ramp:fiat-off-ramp:jibDockerBuild \
  :ledger-accounting:ledger-accounting:jibDockerBuild \
  --parallel
echo "✓ Docker images built"
echo ""

# Step 2: Start Docker Compose stack
echo "▶ Step 2: Starting Docker Compose stack..."
docker compose -f "$COMPOSE_FILE" up -d
echo "✓ Stack started"
echo ""

# Step 3: Wait for services to become healthy
echo "▶ Step 3: Waiting for services to become healthy..."
echo "  (this may take 1-3 minutes)"

services=(
  "p3t-postgres"
  "p3t-redpanda"
  "p3t-temporal"
  "p3t-compliance"
  "p3t-fx-engine"
  "p3t-orchestrator"
  "p3t-onramp"
  "p3t-custody"
  "p3t-offramp"
  "p3t-ledger"
)

max_wait=180
elapsed=0
all_healthy=false

while [ $elapsed -lt $max_wait ]; do
  all_healthy=true
  for svc in "${services[@]}"; do
    status=$(docker inspect --format='{{.State.Health.Status}}' "$svc" 2>/dev/null || echo "missing")
    if [ "$status" != "healthy" ]; then
      all_healthy=false
      break
    fi
  done

  if $all_healthy; then
    break
  fi

  sleep 5
  elapsed=$((elapsed + 5))
  printf "  %ds elapsed...\r" "$elapsed"
done

if ! $all_healthy; then
  echo ""
  echo "✗ Not all services healthy after ${max_wait}s"
  echo "  Service status:"
  for svc in "${services[@]}"; do
    status=$(docker inspect --format='{{.State.Health.Status}}' "$svc" 2>/dev/null || echo "missing")
    printf "    %-25s %s\n" "$svc" "$status"
  done
  echo ""
  echo "Check logs: docker compose -f $COMPOSE_FILE logs <service>"
  exit 1
fi

echo ""
echo "✓ All services healthy"
echo ""

# Step 4: Run tests
echo "▶ Step 4: Running Phase 3 E2E tests..."
cd "$PROJECT_ROOT"
PHASE3_TESTS_ENABLED=true ./gradlew :phase3-integration-tests:test --info
test_exit=$?

echo ""

# Step 5: Print results
if [ $test_exit -eq 0 ]; then
  echo "═══════════════════════════════════════════════════════════"
  echo "  ✓ Phase 3 E2E Tests PASSED"
  echo "═══════════════════════════════════════════════════════════"
else
  echo "═══════════════════════════════════════════════════════════"
  echo "  ✗ Phase 3 E2E Tests FAILED"
  echo "  Report: phase3-integration-tests/build/reports/tests/test/index.html"
  echo "═══════════════════════════════════════════════════════════"
fi

echo ""
echo "Stack is still running. To tear down:"
echo "  docker compose -f docker-compose.phase3-test.yml down -v"
echo ""

exit $test_exit
