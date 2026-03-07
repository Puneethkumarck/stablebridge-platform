# S2 Compliance & Travel Rule -- Context

> Last verified: 2026-03-07 | 157 unit + 16 integration tests

## Purpose

Regulatory gate for the platform. Performs KYC verification, sanctions/AML screening,
risk scoring, and packages FATF Travel Rule data for VASP-to-VASP transmission.
No value moves until this service grants approval.

## Modules

| Module | Type | Purpose |
|--------|------|---------|
| `compliance-travel-rule-api` | java-library | Request/response DTOs (ApiError) |
| `compliance-travel-rule-client` | java-library | Feign client for other services |
| `compliance-travel-rule` | Spring Boot app | Domain + infrastructure + application (port 8083) |

## Package Layout

```
com.stablecoin.payments.compliance
+-- ComplianceTravelRuleApplication.java
+-- application/
|   +-- controller/
|   |   +-- GlobalExceptionHandler.java
|   |   +-- ErrorCodes.java                (CO-XXXX prefix)
|   |   +-- ApiError.java
|   +-- security/
|   |   +-- Roles.java
|   |   +-- SecurityConfig.java            (@ConditionalOnProperty)
|   +-- filter/
|   |   +-- CorrelationIdFilter.java
|   |   +-- IdempotencyKeyFilter.java
|   +-- config/
|       +-- OpenApiConfig.java
+-- domain/
|   +-- model/
|   |   +-- ComplianceCheck.java           (aggregate root)
|   |   +-- ComplianceCheckStatus.java     (10-state enum)
|   |   +-- OverallResult.java
|   |   +-- RiskBand.java, RiskScore.java
|   |   +-- KycResult.java, KycTier.java, KycStatus.java
|   |   +-- SanctionsResult.java
|   |   +-- AmlResult.java
|   |   +-- TravelRulePackage.java, TravelRuleProtocol.java, TransmissionStatus.java
|   |   +-- VaspInfo.java
|   |   +-- CustomerRiskProfile.java
|   +-- port/
|   |   +-- ComplianceCheckRepository.java
|   |   +-- CustomerRiskProfileRepository.java
|   |   +-- EventPublisher.java
|   |   +-- KycProvider.java
|   |   +-- SanctionsProvider.java
|   |   +-- AmlProvider.java
|   |   +-- TravelRuleProvider.java
|   +-- event/
|   |   +-- ComplianceCheckPassed.java
|   |   +-- ComplianceCheckFailed.java
|   |   +-- SanctionsHitEvent.java
|   +-- service/                           (empty -- STA-81)
|   +-- statemachine/                      (empty -- STA-81)
+-- infrastructure/
    +-- config/
    |   +-- FallbackAdaptersConfig.java     (fallback KYC, sanctions, AML, travel rule)
    +-- messaging/
    |   +-- OutboxEventPublisher.java       (Namastack outbox)
    |   +-- ComplianceOutboxHandler.java    (Kafka relay)
    +-- persistence/
        |   +-- entity/
        |   |   +-- ComplianceCheckEntity.java
        |   |   +-- KycResultEntity.java
        |   |   +-- SanctionsResultEntity.java
        |   |   +-- AmlResultEntity.java
        |   |   +-- TravelRulePackageEntity.java    (VaspInfoJson inner record, BYTEA data)
        |   |   +-- CustomerRiskProfileEntity.java
        |   |   +-- ComplianceCheckJpaRepository.java
        |   |   +-- KycResultJpaRepository.java
        |   |   +-- SanctionsResultJpaRepository.java
        |   |   +-- AmlResultJpaRepository.java
        |   |   +-- TravelRulePackageJpaRepository.java
        |   |   +-- CustomerRiskProfileJpaRepository.java
        |   +-- mapper/
        |   |   +-- ComplianceCheckPersistenceMapper.java
        |   |   +-- KycResultPersistenceMapper.java
        |   |   +-- SanctionsResultPersistenceMapper.java
        |   |   +-- AmlResultPersistenceMapper.java
        |   |   +-- TravelRulePackagePersistenceMapper.java
        |   |   +-- CustomerRiskProfilePersistenceMapper.java
        |   |   +-- ComplianceCheckEntityUpdater.java
        |   |   +-- CustomerRiskProfileEntityUpdater.java
        |   +-- ComplianceCheckPersistenceAdapter.java
        |   +-- CustomerRiskProfilePersistenceAdapter.java
```

## State Machine (10 states)

```
PENDING -> KYC_IN_PROGRESS -> SANCTIONS_SCREENING -> AML_SCREENING
    -> RISK_SCORING -> TRAVEL_RULE_PACKAGING -> PASSED
Failure paths: FAILED, SANCTIONS_HIT, MANUAL_REVIEW
```

## API Contract (planned)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/v1/compliance/check` | Initiate compliance check |
| `GET`  | `/v1/compliance/checks/{id}` | Get check status |
| `GET`  | `/v1/customers/{id}/risk-profile` | Get customer risk profile |

Server port: `8083`, context-path: `/compliance`

## Kafka Events Produced (via Namastack outbox)

| Event | Topic |
|-------|-------|
| `ComplianceCheckPassed` | `compliance.result` |
| `ComplianceCheckFailed` | `compliance.result` |
| `SanctionsHitEvent` | `sanctions.hit` |

## Database (Flyway V1 + V2 + V3)

Database: `s2_compliance` on PostgreSQL (`localhost:5432`).

Key tables: `compliance_checks`, `kyc_results`, `sanctions_results`, `aml_results`,
`travel_rule_packages`, `customer_risk_profiles`.

Namastack outbox tables (V3): `compliance_outbox_record`, `compliance_outbox_instance`,
`compliance_outbox_partition`.

## Error Codes

| Code | Meaning |
|------|---------|
| `CO-0001` | Validation error / bad request |
| `CO-0002` | Check not found |
| `CO-0003` | Duplicate payment (check already exists) |
| `CO-0004` | Invalid state transition |
| `CO-0005` | Corridor not supported |
| `CO-0050` | Internal server error |

## Adapter Wiring

- **Real adapters**: `@ConditionalOnProperty` (to be added in STA-82)
- **Mock adapters**: plain POJOs registered by `FallbackAdaptersConfig` via `@ConditionalOnMissingBean`
- No `@Profile` annotations in `src/main`

## Key Decisions

| Decision | Choice |
|----------|--------|
| Outbox | Namastack (`compliance_` prefix) |
| JSONB | Native Hibernate 7 `@JdbcTypeCode(SqlTypes.JSON)` |
| State machine | Custom generic `StateMachine<S,T>` (same as S11) |
| Domain purity | ArchUnit allows `stereotype`, `transaction`, `beans.factory.annotation` in domain |
| Adapter activation | `@ConditionalOnProperty` (not `@Profile`) |
| Error codes | CO-XXXX prefix |
| Enum mapping | `@Enumerated(EnumType.STRING)` (VARCHAR, not PG enum types) |
| Optimistic locking | `@Version` on ComplianceCheckEntity, CustomerRiskProfileEntity |
| Entity updater | MapStruct `@MappingTarget` (ignores version, PK, createdAt) |

## Test Summary

| Category | Count |
|----------|-------|
| Unit (149 domain + 8 ArchUnit) | 157 |
| Integration (11 adapter + 5 profile) | 16 |
| Business | 0 |
| **Total** | **173** |

## Implementation Status

- Scaffolding: Done (STA-80)
- Domain model: Done (STA-81) — ComplianceCheck aggregate (11-transition state machine, 10 states), 2 domain services, 5 VOs, 5 events, 7 ports
- Domain unit tests: Done (STA-82) — 157 tests (149 domain + 8 ArchUnit)
- JPA entities & persistence adapters: Done (STA-83) — 6 entities, 6 repos, 6 mappers, 2 updaters, 2 adapters, V2 migration
- Infrastructure persistence tests: Done (STA-84) — 16 ITs (ComplianceCheckPersistenceAdapterIT + CustomerRiskProfilePersistenceAdapterIT), V3 Namastack outbox fix, KycResultEntity @Builder.Default fix
