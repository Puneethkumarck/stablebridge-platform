package com.stablecoin.payments.ledger.infrastructure.messaging;

import com.stablecoin.payments.ledger.api.events.ReconciliationCompletedEvent;
import com.stablecoin.payments.ledger.api.events.ReconciliationDiscrepancyEvent;
import com.stablecoin.payments.ledger.domain.event.ReconciliationCompletedDomainEvent;
import com.stablecoin.payments.ledger.domain.event.ReconciliationDiscrepancyDomainEvent;
import com.stablecoin.payments.ledger.domain.model.ReconciliationStatus;
import io.namastack.outbox.Outbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.stablecoin.payments.platform.test.TestUtils.eqIgnoring;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class LedgerOutboxEventPublisherTest {

    private static final UUID REC_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID PAYMENT_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final Instant NOW = Instant.parse("2026-03-11T10:00:00Z");

    private Outbox outbox;
    private LedgerOutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        outbox = mock(Outbox.class);
        publisher = new LedgerOutboxEventPublisher(outbox);
    }

    @Nested
    @DisplayName("publishReconciliationCompleted")
    class PublishCompleted {

        @Test
        @DisplayName("should schedule outbox event with correct payload and key")
        void schedulesOutboxEvent() {
            var domainEvent = new ReconciliationCompletedDomainEvent(
                    REC_ID, PAYMENT_ID, ReconciliationStatus.RECONCILED, NOW);

            publisher.publishReconciliationCompleted(domainEvent);

            var expectedEvent = new ReconciliationCompletedEvent(
                    ReconciliationCompletedEvent.SCHEMA_VERSION,
                    null,
                    ReconciliationCompletedEvent.EVENT_TYPE,
                    REC_ID, PAYMENT_ID, "RECONCILED", NOW);
            then(outbox).should().schedule(eqIgnoring(expectedEvent, "eventId"),
                    argThat((String s) -> PAYMENT_ID.toString().equals(s)));
        }
    }

    @Nested
    @DisplayName("publishReconciliationDiscrepancy")
    class PublishDiscrepancy {

        @Test
        @DisplayName("should schedule outbox event with correct payload and key")
        void schedulesOutboxEvent() {
            var domainEvent = new ReconciliationDiscrepancyDomainEvent(
                    REC_ID, PAYMENT_ID, new BigDecimal("1.50"), "USDC",
                    "Stablecoin discrepancy", NOW);

            publisher.publishReconciliationDiscrepancy(domainEvent);

            var expectedEvent = new ReconciliationDiscrepancyEvent(
                    ReconciliationDiscrepancyEvent.SCHEMA_VERSION,
                    null,
                    ReconciliationDiscrepancyEvent.EVENT_TYPE,
                    REC_ID, PAYMENT_ID, new BigDecimal("1.50"), "USDC",
                    "Stablecoin discrepancy", NOW);
            then(outbox).should().schedule(eqIgnoring(expectedEvent, "eventId"),
                    argThat((String s) -> PAYMENT_ID.toString().equals(s)));
        }
    }
}
