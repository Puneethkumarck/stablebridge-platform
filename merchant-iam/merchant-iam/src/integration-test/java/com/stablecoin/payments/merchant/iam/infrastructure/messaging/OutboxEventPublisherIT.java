package com.stablecoin.payments.merchant.iam.infrastructure.messaging;

import com.stablecoin.payments.merchant.iam.AbstractIntegrationTest;
import com.stablecoin.payments.merchant.iam.domain.EventPublisher;
import com.stablecoin.payments.merchant.iam.domain.team.model.events.MerchantUserActivatedEvent;
import com.stablecoin.payments.merchant.iam.domain.team.model.events.MerchantUserInvitedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventPublisherIT extends AbstractIntegrationTest {

    @Autowired
    private EventPublisher<Object> eventPublisher;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate txTemplate;

    @BeforeEach
    void clearOutbox() {
        jdbc.execute("DELETE FROM merchantiam_outbox_record");
    }

    @Test
    void publishes_activated_event_to_outbox() {
        var merchantId = UUID.randomUUID();
        var event = activatedEvent(merchantId);

        txTemplate.executeWithoutResult(status -> eventPublisher.publish(event));

        var count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM merchantiam_outbox_record WHERE record_key = ?",
                Integer.class,
                merchantId.toString());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void outbox_record_payload_contains_email_hash() {
        var emailHash = "abc123hash";
        var merchantId = UUID.randomUUID();
        var event = MerchantUserActivatedEvent.builder()
                .schemaVersion(MerchantUserActivatedEvent.SCHEMA_VERSION)
                .eventId(UUID.randomUUID().toString())
                .eventType(MerchantUserActivatedEvent.EVENT_TYPE)
                .merchantId(merchantId)
                .userId(UUID.randomUUID())
                .emailHash(emailHash)
                .roleId(UUID.randomUUID())
                .roleName("ADMIN")
                .occurredAt(Instant.now())
                .build();

        txTemplate.executeWithoutResult(status -> eventPublisher.publish(event));

        var payload = jdbc.queryForObject(
                "SELECT payload FROM merchantiam_outbox_record LIMIT 1",
                String.class);
        assertThat(payload).contains(emailHash);
    }

    @Test
    void publishes_invited_event_to_outbox() {
        var merchantId = UUID.randomUUID();
        var event = MerchantUserInvitedEvent.builder()
                .schemaVersion(MerchantUserInvitedEvent.SCHEMA_VERSION)
                .eventId(UUID.randomUUID().toString())
                .eventType(MerchantUserInvitedEvent.EVENT_TYPE)
                .invitationId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .merchantId(merchantId)
                .emailHash("def456hash")
                .roleId(UUID.randomUUID())
                .roleName("VIEWER")
                .invitedBy(UUID.randomUUID())
                .occurredAt(Instant.now())
                .build();

        txTemplate.executeWithoutResult(status -> eventPublisher.publish(event));

        var count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM merchantiam_outbox_record WHERE record_key = ?",
                Integer.class,
                merchantId.toString());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void multiple_events_each_produce_a_record() {
        var merchantId = UUID.randomUUID();

        txTemplate.executeWithoutResult(status -> {
            eventPublisher.publish(activatedEvent(merchantId));
            eventPublisher.publish(invitedEvent(merchantId));
        });

        var total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM merchantiam_outbox_record",
                Integer.class);
        assertThat(total).isEqualTo(2);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private MerchantUserActivatedEvent activatedEvent(UUID merchantId) {
        return MerchantUserActivatedEvent.builder()
                .schemaVersion(MerchantUserActivatedEvent.SCHEMA_VERSION)
                .eventId(UUID.randomUUID().toString())
                .eventType(MerchantUserActivatedEvent.EVENT_TYPE)
                .merchantId(merchantId)
                .userId(UUID.randomUUID())
                .emailHash("hash-" + UUID.randomUUID())
                .roleId(UUID.randomUUID())
                .roleName("ADMIN")
                .occurredAt(Instant.now())
                .build();
    }

    private MerchantUserInvitedEvent invitedEvent(UUID merchantId) {
        return MerchantUserInvitedEvent.builder()
                .schemaVersion(MerchantUserInvitedEvent.SCHEMA_VERSION)
                .eventId(UUID.randomUUID().toString())
                .eventType(MerchantUserInvitedEvent.EVENT_TYPE)
                .invitationId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .merchantId(merchantId)
                .emailHash("hash-" + UUID.randomUUID())
                .roleId(UUID.randomUUID())
                .roleName("VIEWER")
                .invitedBy(UUID.randomUUID())
                .occurredAt(Instant.now())
                .build();
    }
}
