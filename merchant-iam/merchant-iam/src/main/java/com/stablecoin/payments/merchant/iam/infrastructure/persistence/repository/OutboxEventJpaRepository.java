package com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository;

import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findTop50ByPublishedFalseOrderByCreatedAtAsc();
}
