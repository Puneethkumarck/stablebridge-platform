package com.stablecoin.payments.merchant.iam.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

public record MerchantActivatedEvent(
        String eventId,
        String eventType,
        UUID merchantId,
        String companyName,
        String primaryContactEmail,
        String primaryContactName,
        Instant activatedAt
) {}
