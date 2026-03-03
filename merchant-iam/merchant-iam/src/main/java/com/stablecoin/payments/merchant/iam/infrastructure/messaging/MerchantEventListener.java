package com.stablecoin.payments.merchant.iam.infrastructure.messaging;

import com.stablecoin.payments.merchant.iam.domain.team.MerchantTeamService;
import com.stablecoin.payments.merchant.iam.domain.team.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantEventListener {

    private final MerchantTeamService merchantTeamService;
    private final UserSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Consumes {@code merchant.activated} from S11.
     * Seeds the 4 built-in roles and creates the first ADMIN user for the merchant.
     */
    @KafkaListener(topics = "merchant.activated", groupId = "merchant-iam-onboard")
    public void onMerchantActivated(@Payload String payload) {
        try {
            var event = objectMapper.readValue(payload, MerchantActivatedEvent.class);
            log.info("Received merchant.activated merchantId={}", event.merchantId());

            merchantTeamService.seedRolesAndFirstAdmin(
                    event.merchantId(),
                    event.primaryContactEmail(),
                    event.primaryContactName(),
                    event.companyName());

            log.info("Seeded roles and first admin for merchantId={}", event.merchantId());
        } catch (Exception e) {
            log.error("Failed to process merchant.activated: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process merchant.activated", e);
        }
    }

    /**
     * Consumes {@code merchant.suspended} from S11.
     * Revokes all active user sessions for the merchant.
     */
    @KafkaListener(topics = "merchant.suspended", groupId = "merchant-iam-suspend")
    public void onMerchantSuspended(@Payload String payload) {
        try {
            var event = objectMapper.readValue(payload, MerchantSuspendedEvent.class);
            log.info("Received merchant.suspended merchantId={}", event.merchantId());

            sessionRepository.revokeAllByMerchantId(event.merchantId(), "merchant_suspended");

            log.info("Revoked all sessions for merchantId={}", event.merchantId());
        } catch (Exception e) {
            log.error("Failed to process merchant.suspended: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process merchant.suspended", e);
        }
    }

    /**
     * Consumes {@code merchant.closed} from S11.
     * Deactivates all users and revokes all sessions for the merchant.
     */
    @KafkaListener(topics = "merchant.closed", groupId = "merchant-iam-close")
    public void onMerchantClosed(@Payload String payload) {
        try {
            var event = objectMapper.readValue(payload, MerchantClosedEvent.class);
            log.info("Received merchant.closed merchantId={}", event.merchantId());

            merchantTeamService.deactivateAllUsers(event.merchantId());
            sessionRepository.revokeAllByMerchantId(event.merchantId(), "merchant_closed");

            log.info("Deactivated all users for merchantId={}", event.merchantId());
        } catch (Exception e) {
            log.error("Failed to process merchant.closed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process merchant.closed", e);
        }
    }
}
