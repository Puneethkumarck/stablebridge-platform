# S11 Merchant Onboarding — Context

> Read this file at session start instead of crawling source files.
> Update this file whenever a significant change is made to this service.

---

## Purpose

Owns the full merchant lifecycle from application through KYB verification to activation.
Produces `merchant.activated` which downstream services (S13 IAM, S10 API Gateway) depend on.
Every payment in the platform requires an `ACTIVE` merchant with an approved corridor.

---

## Modules

| Module | Artifact | Responsibility |
|--------|----------|----------------|
| `merchant-onboarding-api` | `java-library` | Request/response DTOs used by controller and Feign client |
| `merchant-onboarding-client` | `java-library` | Feign client for other services to call S11 |
| `merchant-onboarding` | Spring Boot app | Domain, infrastructure, application — the deployable service |

Gradle coordinates (from root `settings.gradle.kts`):
```
include("merchant-onboarding:merchant-onboarding-api")
include("merchant-onboarding:merchant-onboarding-client")
include("merchant-onboarding:merchant-onboarding")
```

---

## Package Layout

```
com.stablecoin.payments.merchant.onboarding
├── domain/
│   ├── merchant/
│   │   ├── Merchant.java                  ← aggregate root
│   │   ├── MerchantRepository.java        ← outbound port
│   │   ├── MerchantTrigger.java           ← state machine triggers (enum)
│   │   ├── KybProvider.java               ← outbound port
│   │   └── model/
│   │       ├── core/                      ← value objects + enums
│   │       │   ├── MerchantStatus.java    ← 8 states
│   │       │   ├── KybStatus.java
│   │       │   ├── EntityType.java
│   │       │   ├── RateLimitTier.java
│   │       │   ├── RiskTier.java
│   │       │   ├── ContactRole.java
│   │       │   ├── DocumentType.java
│   │       │   ├── DocumentStatus.java
│   │       │   ├── BusinessAddress.java   ← record @Builder
│   │       │   ├── BeneficialOwner.java   ← record @Builder
│   │       │   ├── MerchantContact.java   ← record @Builder
│   │       │   ├── KybVerification.java   ← record @Builder
│   │       │   └── ApprovedCorridor.java  ← record @Builder
│   │       └── events/                    ← 6 domain events (records)
│   ├── statemachine/
│   │   ├── StateMachine.java              ← generic StateMachine<S,T>
│   │   ├── StateTransition.java           ← record<S,T>(from, trigger, to)
│   │   └── StateMachineException.java
│   ├── exceptions/
│   │   ├── MerchantNotFoundException.java
│   │   ├── MerchantAlreadyExistsException.java
│   │   └── InvalidMerchantStateException.java
│   └── EventPublisher.java                ← generic outbound port
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/MerchantEntity.java     ← JPA, JSONB via hypersistence-utils
│   │   ├── entity/MerchantJpaRepository.java
│   │   ├── MerchantRepositoryAdapter.java ← implements MerchantRepository port
│   │   └── mapper/MerchantEntityMapper.java ← MapStruct
│   ├── messaging/
│   │   ├── OutboxMerchantEventPublisher.java ← implements EventPublisher (writes to outbox_events)
│   │   ├── OutboxEvent.java                ← JPA entity for outbox_events table
│   │   ├── OutboxEventRepository.java      ← Spring Data JPA
│   │   └── OutboxRelayJob.java             ← @Scheduled relay: DB → Kafka via KafkaTemplate
│   └── kyb/
│       └── MockKybAdapter.java            ← @Profile("local","test") implements KybProvider
└── application/
    ├── service/
    │   └── MerchantApplicationService.java ← orchestrates all use cases
    └── controller/
        ├── MerchantController.java
        ├── GlobalExceptionHandler.java     ← RFC 9457 ProblemDetail
        └── MerchantRequestResponseMapper.java ← MapStruct
```

---

## Domain Model

### State Machine (12 transitions)

```
                     START_KYB
  APPLIED ──────────────────────────► KYB_IN_PROGRESS
                                           │
                          KYB_PASSED ──────┼──────► PENDING_APPROVAL
                          KYB_FLAGGED ─────┤               │
                          KYB_FAILED ──────┤        APPROVE │
                                           │               ▼
                                    KYB_MANUAL_REVIEW  ACTIVE ──── SUSPEND ──► SUSPENDED
                                           │               │                       │
                                    KYB_PASSED             │◄──── REACTIVATE ──────┘
                                           │               │
                                    PENDING_APPROVAL  CLOSE │
                                                           ▼
                                    KYB_REJECTED ──► CLOSED ◄──── CLOSE (SUSPENDED)
```

### Key types

| Type | Kind | Notes |
|------|------|-------|
| `Merchant` | Aggregate root | `@Builder(access=PACKAGE)`, domain methods enforce state machine |
| `MerchantStatus` | Enum | 8 values: APPLIED, KYB_IN_PROGRESS, KYB_MANUAL_REVIEW, KYB_REJECTED, PENDING_APPROVAL, ACTIVE, SUSPENDED, CLOSED |
| `MerchantTrigger` | Enum | 8 values: START_KYB, KYB_PASSED, KYB_FLAGGED, KYB_FAILED, APPROVE, SUSPEND, REACTIVATE, CLOSE |
| `KybVerification` | Value object (record) | Holds provider ref, risk signals, required documents |
| `ApprovedCorridor` | Value object (record) | sourceCountry, targetCountry, maxAmountUsd, expiresAt |
| `BeneficialOwner` | Value object (record) | `nationalIdRef` is a Vault reference — never the actual ID |

---

## API Contract

| Method | Path | Description | Status code |
|--------|------|-------------|-------------|
| `POST` | `/api/v1/merchants` | Submit merchant application | `201` |
| `GET` | `/api/v1/merchants/{merchantId}` | Get merchant by ID | `200` |
| `POST` | `/api/v1/merchants/{merchantId}/kyb/start` | Submit KYB to provider | `202` |
| `POST` | `/api/v1/merchants/{merchantId}/activate` | Approve and activate | `200` |
| `POST` | `/api/v1/merchants/{merchantId}/suspend` | Suspend active merchant | `202` |
| `POST` | `/api/v1/merchants/{merchantId}/reactivate` | Reactivate suspended merchant | `202` |
| `POST` | `/api/v1/merchants/{merchantId}/corridors` | Approve a payment corridor | `201` |

Header required for corridor approval: `X-Approved-By: <UUID>`

Server port: `8081` (configured in `application.yml`)

---

## Kafka Events Produced

| Event record | Topic | Trigger |
|-------------|-------|---------|
| `MerchantAppliedEvent` | `merchant.applied` | `apply()` |
| `MerchantActivatedEvent` | `merchant.activated` | `activate()` — consumed by S13, S10 |
| `MerchantSuspendedEvent` | `merchant.suspended` | `suspend()` |
| `MerchantKybPassedEvent` | `merchant.kyb.passed` | Future KYB webhook handler |
| `MerchantKybFailedEvent` | `merchant.kyb.failed` | Future KYB webhook handler |
| `MerchantCorridorApprovedEvent` | `merchant.corridor.approved` | `approveCorridor()` |

Events are published via `OutboxMerchantEventPublisher` → `outbox_events` table (transactional, at-least-once). `OutboxRelayJob` (@Scheduled, every 1 s) reads unprocessed rows and publishes to Kafka via `KafkaTemplate`.
`startKyb()` does NOT publish — KYB result arrives via Onfido webhook (not yet implemented).

---

## Database (Flyway V1–V6)

| Migration | Table | Key columns |
|-----------|-------|-------------|
| V1 | `merchants` | `merchant_id UUID PK`, `status merchant_status`, `registered_address JSONB`, `beneficial_owners JSONB`, `version BIGINT` (optimistic lock) |
| V2 | `merchant_contacts` | `contact_id`, `merchant_id FK`, `role contact_role`, `email` |
| V3 | `kyb_verifications` | `kyb_id`, `merchant_id FK`, `provider`, `provider_ref`, `risk_signals JSONB` |
| V4 | `merchant_documents` | `document_id`, `merchant_id FK`, `kyb_id FK`, `document_type`, `s3_key` |
| V5 | `approved_corridors` | `corridor_id`, `merchant_id FK`, `source_country CHAR(2)`, `max_amount_usd NUMERIC`, `expires_at` |
| V6 | `merchant_audit_log` | `log_id`, `merchant_id`, `action`, `previous_state JSONB`, `new_state JSONB` |

Database: `s11_merchant_onboarding` on PostgreSQL (local: `localhost:5432` via Docker Compose).
| V7 | `outbox_events` | `id UUID PK`, `topic`, `event_type`, `payload TEXT`, `created_at`, `processed BOOL`, `processed_at`, `retry_count` |

---

## Key Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Outbox pattern | Inline JPA outbox (`OutboxEvent` entity + `OutboxRelayJob` @Scheduled) | namastack-outbox not on Maven Central; self-contained, no external lib |
| JSONB fields | `hypersistence-utils-hibernate-63` `JsonType` | `registered_address` and `beneficial_owners` are nested — JSONB avoids join tables |
| KYB result | Webhook handler (pending) not inline | Onfido is async; webhook triggers `kybPassed()` / `kybFailed()` domain methods |
| Domain purity | Zero Spring/JPA in `domain/` package | Enforced by ArchUnit (`ArchitectureTest`) |
| Controller location | `application.controller` | No `web/` package — preference rule |
| State machine | Custom generic `StateMachine<S,T>` | Avoids Spring State Machine dependency in domain layer |
| Beneficial owner ID | Vault reference string | PII — actual national ID stored in HashiCorp Vault, not in DB |

---

## Completion Status

| Layer | Status | Notes |
|-------|--------|-------|
| Domain model | ✅ Complete | All value objects, state machine, events, exceptions |
| Infrastructure — JPA | ✅ Complete | Entity, adapter, MapStruct mapper |
| Infrastructure — KYB | ⚠️ Mock only | `MockKybAdapter` for local/test; real Onfido adapter pending |
| Infrastructure — Messaging | ✅ Complete | `OutboxMerchantEventPublisher` (JPA) + `OutboxRelayJob` (Kafka) |
| Application service | ✅ Complete | 7 use cases |
| REST controller | ✅ Complete | 7 endpoints, ProblemDetail error handling |
| Flyway migrations | ✅ Complete | V1–V6 |
| KYB webhook handler | ❌ Pending | Onfido POST callback → `kybPassed()` / `kybFailed()` |
| Real Onfido adapter | ❌ Pending | Needs Onfido sandbox key |
| Integration tests | ❌ Pending | TestContainers + WireMock stubs |
| Gradle build verified | ❌ Pending | `./gradlew :merchant-onboarding:merchant-onboarding:build` |

---

## Extension Pattern

To add a new use case (e.g. "close merchant"):

1. **Domain** — add trigger to `MerchantTrigger`, add transition to `Merchant.STATE_MACHINE`, add domain method to `Merchant`
2. **Event** — add `MerchantClosedEvent` record with `TOPIC` / `EVENT_TYPE` constants
3. **Service** — add method to `MerchantApplicationService`, call `eventPublisher.publish()`
4. **Controller** — add endpoint to `MerchantController`
5. **Test** — add `@Nested` scenario to `MerchantTest`

To add a new infrastructure adapter (e.g. real Onfido):

1. Implement `KybProvider` port in `infrastructure/kyb/`
2. Annotate with `@Profile("prod")` (mock stays `@Profile("local","test")`)
3. No changes needed to domain or application layer
