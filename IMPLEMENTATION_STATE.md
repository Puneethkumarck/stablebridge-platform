# Implementation State

> Last updated: 2026-03-07
> Status: Phase 2 in progress. S2 persistence tests done. S6 persistence tests next.
> Scope: Local development & testing only

---

## Current Phase

**Phase 2 — Core Payment Logic**
Goal: S1 Payment Orchestrator, S2 Compliance & Travel Rule, S6 FX & Liquidity Engine.
Sequence: S2 + S6 (parallel) → S1 (depends on S2+S6 API modules) → Integration Testing.

## Current Milestone

**Phase 2 — S2 persistence tests done. S6 persistence tests next.**
Phase 1 complete (650 tests, 3 services). STA-9 (Phase 1 E2E) deferred.
Phase 2 Linear issues: 37 sub-tasks across 3 parent issues (STA-10, STA-11, STA-12).
STA-80–84, STA-93–96 done. Next: STA-97 (S6 persistence tests).

## Current Task

**ID**: STA-97
**Title**: S6-05: Infrastructure persistence tests
**Status**: not_started
**Depends on**: STA-96 (done)

## Completed Tasks

| ID | Task | Completed | Evidence |
|----|------|-----------|---------|
| STA-5 | Infrastructure Foundation — Docker Compose local dev stack | 2026-03-01 | `docker-compose.dev.yml`, CI/CD pipeline |
| STA-6 | S11 Merchant Onboarding — full implementation | 2026-03-02 | 107 tests green, hexagonal arch, Temporal workflow |
| STA-7 | S13 Merchant IAM & Role Management (parent) | 2026-03-03 | All sub-tasks done (STA-26 through STA-37) |
| STA-26 | S13-01: Project scaffold | 2026-03-02 | 3 Gradle modules, 7 Flyway migrations |
| STA-27 | S13-02: JPA entities, repositories, adapters | 2026-03-03 | 6 entities, 6 repos, 22 IT + 5 ArchUnit |
| STA-28 | S13-03: Infrastructure persistence tests | 2026-03-03 | Repository integration tests |
| STA-29 | S13-04: Domain model | 2026-03-03 | 41 domain files, zero Spring/JPA imports (except allowed stereotype, transaction, beans.factory.annotation) |
| STA-30 | S13-05: Domain model unit tests | 2026-03-03 | 109 unit tests, all invariants covered |
| STA-31 | S13-06: Auth infrastructure (JWT, bcrypt, TOTP, Redis) | 2026-03-03 | ES256 JWT, TOTP MFA, Redis cache |
| STA-32 | S13-07: Messaging (Kafka consumers, outbox) | 2026-03-03 | merchant.activated consumer, outbox publisher |
| STA-33 | S13-08: Infrastructure tests | 2026-03-03 | Auth + messaging integration tests |
| STA-34 | S13-09: Application services | 2026-03-03 | User, role, auth, session management |
| STA-35 | S13-10: Controllers | 2026-03-03 | REST endpoints + exception handler |
| STA-36 | S13-11: Application layer tests | 2026-03-03 | Service + controller tests |
| STA-37 | S13-12: Business test | 2026-03-03 | E2E: merchant.activated → invite → login → permission check |
| STA-8 | S10 API Gateway & IAM (parent) | 2026-03-04 | All sub-tasks done (STA-38 through STA-44) |
| STA-38 | S10: Project Scaffold | 2026-03-03 | 3 Gradle modules, 7 Flyway migrations |
| STA-39 | S10: Domain Model | 2026-03-03 | Records, ports, services |
| STA-40 | S10: Domain Unit Tests | 2026-03-03 | 111 unit tests, ArchUnit rules |
| STA-41 | S10: Persistence Layer + ITs | 2026-03-03 | JPA entities, repos, 35 ITs |
| STA-42 | S10: Auth + Cache + Messaging | 2026-03-03 | JWT, Redis, Kafka consumers, outbox |
| STA-43 | S10: Application Layer | 2026-03-03 | Controllers, DTOs, tests |
| STA-44 | S10: Business Tests | 2026-03-04 | 11 E2E business tests |
| STA-45 | S10: Request Pipeline Filters | 2026-03-04 | JWT, API Key, Rate Limit, Audit filters |
| STA-46 | S10 ↔ S13: JWKS Integration | 2026-03-04 | Gateway validates user JWTs via S13 JWKS |
| STA-47 | S13: Fix JWT principal extraction + logout | 2026-03-04 | JwtAuthenticationFilter, real callerId |
| STA-48 | S13: Missing endpoints (refresh, MFA, Redis fix) | 2026-03-04 | Refresh token, MFA setup/activate, Redis evictAll |
| STA-49 | S10: OAuth client auto-provisioning | 2026-03-04 | Auto-provision on merchant.activated, admin endpoint, Feign client |
| STA-50 | Phase 1 Alignment & Fix | 2026-03-04 | S13 BT fix, S11 Namastack outbox, S11 ApiError, IdempotencyKeyFilter S11+S10 |
| STA-51 | Fix S13 business test failures | 2026-03-04 | TestSecurityConfig UserAuthentication filter, Redis cache key fix |
| STA-52 | Migrate S11 outbox to Namastack | 2026-03-04 | Namastack outbox replaces custom OutboxEvent/OutboxRelayJob |
| STA-53 | Standardize S11 error response to ApiError | 2026-03-04 | MO-XXXX codes, ApiError record, field-level validation errors |
| STA-54 | Add IdempotencyKeyFilter to S11 and S10 | 2026-03-04 | Idempotency-Key header required for POST/PATCH/DELETE |
| STA-55 | Update CONTEXT.md test counts | 2026-03-04 | Test counts refreshed after all fixes |
| STA-67 | Add springdoc-openapi to Phase 1 services | 2026-03-05 | OpenAPI docs for S10, S11, S13 |
| STA-73 | Replace @Profile with @ConditionalOnProperty | 2026-03-06 | All services migrated, no @Profile in src/main |
| STA-69 | S10 merchant-scoped authorization with @PreAuthorize | 2026-03-06 | MerchantScopeEnforcer, SecurityExpressions, GW-2003 |
| STA-70 | S13 make session revocation authoritative | 2026-03-06 | Session persisted on login, refresh validates session |
| STA-74 | S11 add GET /api/v1/merchants list endpoint | 2026-03-06 | Pagination, status filter, PagedResult, PageResponse |
| STA-80 | S2-01: Project scaffold | 2026-03-07 | 3 Gradle modules, Flyway V1 (7 tables), 8 ArchUnit tests |
| STA-93 | S6-01: Project scaffold | 2026-03-07 | 3 Gradle modules, Flyway V1 (6 tables + TimescaleDB hypertable), 7 ArchUnit tests |
| STA-81 | S2-02: Domain model — aggregates, VOs, state machine, events, services | 2026-03-07 | 14 new files, 3 updated; 8 ArchUnit tests pass; zero Spring/JPA imports (except allowed stereotype, transaction, beans.factory.annotation) |
| STA-94 | S6-02: Domain model — aggregates, VOs, state machines, domain services | 2026-03-07 | 10 new files, 3 updated; 7 ArchUnit tests pass |
| STA-82 | S2-03: Domain model unit tests | 2026-03-07 | 157 tests (149 new + 8 ArchUnit), all pass |
| STA-95 | S6-03: Domain model unit tests | 2026-03-07 | 121 tests (114 new + 7 ArchUnit), all pass |
| STA-83 | S2-04: Infrastructure persistence | 2026-03-07 | 6 entities, 6 repos, 6 mappers, 2 updaters, 2 adapters, V2 migration |
| STA-96 | S6-04: Infrastructure persistence | 2026-03-07 | 4 entities, 4 repos, 3 mappers, 3 updaters, 3 adapters, V2 migration |
| STA-84 | S2-05: Infrastructure persistence tests | 2026-03-07 | 16 ITs (11 adapter + 5 profile), V3 outbox fix, @Builder.Default fix |

## Pending Tasks

| ID | Task | Phase | Depends On |
|----|------|-------|-----------|
| STA-9 | Phase 1 Integration Testing — E2E merchant flow | Phase 1 | STA-6, STA-7, STA-8 (all done) |
| STA-10 | S1 Payment Orchestrator — Temporal saga | Phase 2 | STA-9 |
| STA-11 | S2 Compliance & Travel Rule | Phase 2 | STA-10 |
| STA-12 | S6 FX & Liquidity Engine | Phase 2 | STA-10 |
| STA-13 | Phase 2 Integration Testing | Phase 2 | STA-10, STA-11, STA-12 |

## Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-03-01 | PostgreSQL, not MySQL | Platform spec uses Postgres |
| 2026-03-01 | Local dev only (Docker Compose) | User direction |
| 2026-03-02 | `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` not `PostgreSQLEnumType` | Hibernate 6.3+ native support |
| 2026-03-02 | `MerchantEntityUpdater` for hexagonal JPA updates | Prevents version/session conflicts |
| 2026-03-02 | Disable JaCoCo on IT/business tests | JaCoCo incompatible with Java 25 |
| 2026-03-02 | TestContainers singleton for ITs | Single PG + Kafka container shared |
| 2026-03-02 | `FallbackAdaptersConfig` + `@ConditionalOnMissingBean` for mocks | Clean fallback pattern |
| 2026-03-02 | Temporal for S11 KYB, not for S13 | S13 has no long-running workflows |
| 2026-03-02 | Email: BYTEA (AES-256) + SHA-256 hash | GDPR; hash enables search |
| 2026-03-02 | Redis for permission cache | < 10ms p99 permission checks |
| 2026-03-03 | Namastack outbox replaces custom outbox for S13 | Spring Boot 4 compatible |
| 2026-03-03 | MerchantTeam as rich aggregate | Small team sizes; in-memory invariants |
| 2026-03-06 | `@ConditionalOnProperty` replaces `@Profile` | Testable without profile activation; explicit property-based feature toggling |
| 2026-03-06 | `@PreAuthorize` for merchant-scoped authorization in S10 | `MerchantScopeEnforcer` validates principal→merchant binding on every endpoint |
| 2026-03-06 | Session persistence on login (S13) | Refresh tokens validated against session state; logout/suspend immediately effective |

## Resume Snapshot

- Phase 2 in progress. S2 persistence tests done (STA-84). S6 persistence tests next (STA-97).
- S2: 173 tests (157 unit + 16 integration). V3 migration fixed Namastack outbox tables.
- S6: 121 tests + persistence layer (4 entities, 4 repos, 3 mappers, 3 updaters, 3 adapters, V2 migration).
- Phase 1 intact: S11 (104), S13 (248), S10 (298). Grand total: 944 tests.
- Next: STA-97 (S6 persistence tests). S6 also needs outbox table fix (same pattern as S2 V3).
