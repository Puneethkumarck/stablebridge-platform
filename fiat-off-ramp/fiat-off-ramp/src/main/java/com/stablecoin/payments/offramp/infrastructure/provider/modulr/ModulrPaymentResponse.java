package com.stablecoin.payments.offramp.infrastructure.provider.modulr;

import java.math.BigDecimal;

record ModulrPaymentResponse(
        String id,
        String status,
        String createdDate,
        String externalReference,
        String approvalStatus,
        String message,
        ModulrPaymentDetails details
) {

    record ModulrPaymentDetails(
            String sourceAccountId,
            String destinationType,
            ModulrDestinationDetails destination,
            BigDecimal amount,
            String reference
    ) {}

    record ModulrDestinationDetails(
            String type,
            String iban,
            String name
    ) {}
}
