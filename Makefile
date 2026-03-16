.PHONY: help build clean test test-unit test-integration test-business \
       format lint check \
       infra-up infra-down infra-destroy infra-status infra-logs infra-logs-% \
       run-% db-reset db-psql topics \
       deps outdated \
       assemble sonar fresh ci \
       e2e-up e2e-down e2e-destroy e2e-status e2e-build e2e-test e2e

# ─────────────────────────────────────────────
# Variables
# ─────────────────────────────────────────────
GRADLE       := ./gradlew
COMPOSE      := docker compose -f docker-compose.dev.yml
SERVICES     := merchant-onboarding merchant-iam api-gateway-iam \
                compliance-travel-rule fx-liquidity-engine payment-orchestrator \
                fiat-on-ramp blockchain-custody fiat-off-ramp ledger-accounting

# ─────────────────────────────────────────────
# Help
# ─────────────────────────────────────────────
help: ## Show this help
	@grep -E '^[a-zA-Z_%-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-22s\033[0m %s\n", $$1, $$2}'

# ─────────────────────────────────────────────
# Build
# ─────────────────────────────────────────────
build: ## Build all modules (skip tests)
	$(GRADLE) build -x test -x integrationTest -x businessTest

clean: ## Clean all build artifacts
	$(GRADLE) clean

assemble: ## Compile and assemble JARs (no tests, no checks)
	$(GRADLE) assemble

# ─────────────────────────────────────────────
# Build single service: make build-<service>
# ─────────────────────────────────────────────
build-%: ## Build a single service (e.g., make build-merchant-iam)
	$(GRADLE) :$*:$*:build -x test -x integrationTest -x businessTest

# ─────────────────────────────────────────────
# Test
# ─────────────────────────────────────────────
test: ## Run all tests (unit + integration + business)
	$(GRADLE) test integrationTest businessTest

test-unit: ## Run unit tests only
	$(GRADLE) test

test-integration: ## Run integration tests only (requires infra)
	$(GRADLE) integrationTest

test-business: ## Run business tests only (requires infra)
	$(GRADLE) businessTest

# ─────────────────────────────────────────────
# Test single service: make test-<service>
# ─────────────────────────────────────────────
test-%-unit: ## Unit tests for a service (e.g., make test-merchant-iam-unit)
	$(GRADLE) :$*:$*:test

test-%-integration: ## Integration tests for a service (e.g., make test-merchant-iam-integration)
	$(GRADLE) :$*:$*:integrationTest

test-%-business: ## Business tests for a service (e.g., make test-merchant-iam-business)
	$(GRADLE) :$*:$*:businessTest

test-%-all: ## All tests for a service (e.g., make test-merchant-iam-all)
	$(GRADLE) :$*:$*:test :$*:$*:integrationTest :$*:$*:businessTest

# ─────────────────────────────────────────────
# Code Quality
# ─────────────────────────────────────────────
format: ## Apply Spotless formatting
	$(GRADLE) spotlessApply

lint: ## Check Spotless formatting (CI)
	$(GRADLE) spotlessCheck

check: ## Full CI check — build + format check + all tests (excludes E2E)
	$(GRADLE) check -x :phase2-integration-tests:test -x :phase3-integration-tests:test --build-cache --parallel

sonar: ## Run SonarCloud analysis
	$(GRADLE) sonar

# ─────────────────────────────────────────────
# Infrastructure (Docker Compose)
# ─────────────────────────────────────────────
infra-up: ## Start all infrastructure containers
	$(COMPOSE) up -d

infra-down: ## Stop all infrastructure containers
	$(COMPOSE) down

infra-destroy: ## Stop containers and remove volumes (full reset)
	$(COMPOSE) down -v

infra-status: ## Show infrastructure container status
	$(COMPOSE) ps

infra-logs: ## Tail infrastructure logs (all services)
	$(COMPOSE) logs -f --tail=100

infra-logs-%: ## Tail logs for a specific container (e.g., make infra-logs-postgres)
	$(COMPOSE) logs -f --tail=100 $*

# ─────────────────────────────────────────────
# Database
# ─────────────────────────────────────────────
db-reset: ## Drop and recreate all databases (destructive)
	$(COMPOSE) down -v postgres
	$(COMPOSE) up -d --wait postgres
	@echo "Databases recreated from init.sql"

db-psql: ## Open psql shell to local PostgreSQL
	$(COMPOSE) exec postgres psql -U dev -d postgres

# ─────────────────────────────────────────────
# Kafka (Redpanda)
# ─────────────────────────────────────────────
topics: ## List all Kafka topics
	$(COMPOSE) exec redpanda rpk topic list

# ─────────────────────────────────────────────
# Run a service locally
# ─────────────────────────────────────────────
run-%: ## Run a service with dev profile (e.g., make run-merchant-iam)
	$(GRADLE) :$*:$*:bootRun --args='--spring.profiles.active=dev'

# ─────────────────────────────────────────────
# Dependencies
# ─────────────────────────────────────────────
deps: ## Show dependency tree for all modules
	$(GRADLE) dependencies --configuration runtimeClasspath

outdated: ## Check for outdated dependencies
	$(GRADLE) dependencyUpdates -Drevision=release 2>/dev/null || \
		echo "Install com.github.ben-manes.versions plugin for this target"

# ─────────────────────────────────────────────
# Convenience
# ─────────────────────────────────────────────
fresh: clean build ## Clean + build

ci: lint test ## Full CI pipeline (format check + all tests)

# ─────────────────────────────────────────────
# E2E Tests (require Docker Compose stack)
# ─────────────────────────────────────────────
E2E_COMPOSE  := docker compose -f docker-compose.phase3-test.yml

e2e-up: ## Start Phase 3 E2E Docker Compose stack
	$(E2E_COMPOSE) up -d

e2e-down: ## Stop Phase 3 E2E stack
	$(E2E_COMPOSE) down

e2e-destroy: ## Stop E2E stack and remove volumes
	$(E2E_COMPOSE) down -v

e2e-status: ## Show E2E stack container status
	$(E2E_COMPOSE) ps

e2e-build: ## Build all service Docker images for E2E
	$(GRADLE) $(foreach s,$(SERVICES),:$(s):$(s):jibDockerBuild) --parallel

e2e-test: ## Run Phase 3 E2E tests (stack must be running)
	PHASE3_TESTS_ENABLED=true $(GRADLE) :phase3-integration-tests:test --rerun

e2e: e2e-build e2e-up e2e-test ## Build images, start stack, run E2E tests
