package com.stablecoin.payments.merchant.onboarding.infrastructure.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop50ByProcessedFalseOrderByCreatedAtAsc();
}
