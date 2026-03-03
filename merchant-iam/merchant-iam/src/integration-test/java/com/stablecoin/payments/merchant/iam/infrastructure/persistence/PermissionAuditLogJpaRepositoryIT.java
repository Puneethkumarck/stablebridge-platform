package com.stablecoin.payments.merchant.iam.infrastructure.persistence;

import com.stablecoin.payments.merchant.iam.AbstractIntegrationTest;
import com.stablecoin.payments.merchant.iam.fixtures.IamEntityFixtures;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.PermissionAuditLogJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PermissionAuditLogJpaRepository IT")
class PermissionAuditLogJpaRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private PermissionAuditLogJpaRepository auditLogJpaRepository;

    @Test
    @DisplayName("should find audit logs by merchant id (paginated)")
    void shouldFindByMerchantId() {
        var entry1 = IamEntityFixtures.anAuditLogEntry();
        var entry2 = IamEntityFixtures.anAuditLogEntry();
        auditLogJpaRepository.save(entry1);
        auditLogJpaRepository.save(entry2);

        var page = auditLogJpaRepository.findByMerchantIdOrderByOccurredAtDesc(
                IamEntityFixtures.defaultMerchantId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().getFirst().getAction()).isEqualTo("ROLE_CHANGED");
    }

    @Test
    @DisplayName("should find audit logs by user id")
    void shouldFindByUserId() {
        var entry = IamEntityFixtures.anAuditLogEntry();
        auditLogJpaRepository.save(entry);

        var page = auditLogJpaRepository.findByUserIdOrderByOccurredAtDesc(
                entry.getUserId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("should persist JSONB detail field")
    void shouldPersistJsonbDetail() {
        var entry = IamEntityFixtures.anAuditLogEntry();
        auditLogJpaRepository.save(entry);

        var found = auditLogJpaRepository.findById(entry.getLogId());

        assertThat(found).isPresent();
        assertThat(found.get().getDetail()).contains("VIEWER");
        assertThat(found.get().getDetail()).contains("ADMIN");
    }

    @Test
    @DisplayName("should return empty page for unknown merchant")
    void shouldReturnEmptyForUnknownMerchant() {
        var page = auditLogJpaRepository.findByMerchantIdOrderByOccurredAtDesc(
                UUID.randomUUID(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isZero();
    }
}
