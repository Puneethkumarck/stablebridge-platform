package com.stablecoin.payments.offramp.infrastructure.provider.modulr;

import java.math.BigDecimal;

/**
 * ACL DTO for Modulr Create Payment API response.
 * Package-private — never leaks to domain.
 *
 * @param id                Modulr payment ID (e.g., "P120003AQM")
 * @param status            payment status (e.g., "VALIDATED")
 * @param createdDate       ISO 8601 creation timestamp
 * @param externalReference the external reference echoed back
 * @param approvalStatus    approval status (e.g., "NOTNEEDED")
 * @param message           optional status message
 * @param details           payment details
 */
record ModulrPaymentResponse(
        String id,
        String status,
        String createdDate,
        String externalReference,
        String approvalStatus,
        String message,
        ModulrPaymentDetails details
) {

    /**
     * Payment details nested in the Modulr response.
     *
     * @param sourceAccountId  source account used
     * @param destinationType  destination type (e.g., "IBAN")
     * @param destination      beneficiary destination
     * @param amount           payment amount
     * @param reference        payment reference
     */
    record ModulrPaymentDetails(
            String sourceAccountId,
            String destinationType,
            ModulrDestinationDetails destination,
            BigDecimal amount,
            String reference
    ) {}

    /**
     * Destination details in the response.
     *
     * @param type destination type
     * @param iban beneficiary IBAN
     * @param name beneficiary name
     */
    record ModulrDestinationDetails(
            String type,
            String iban,
            String name
    ) {}
}
