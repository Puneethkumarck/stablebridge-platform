package com.stablecoin.payments.offramp.infrastructure.provider.modulr;

import java.math.BigDecimal;

/**
 * ACL DTO for Modulr Create Payment API request.
 * Package-private — never leaks to domain.
 * <p>
 * Maps to: {@code POST /api-sandbox-token/payments}
 *
 * @param sourceAccountId  Modulr source account ID
 * @param amount           payment amount
 * @param currency         ISO 4217 currency code (e.g., EUR)
 * @param reference        payment reference visible to beneficiary
 * @param externalReference idempotency key / external correlation ID
 * @param destination      beneficiary destination details
 * @param permittedScheme  payment scheme (e.g., SEPA_CREDIT)
 */
record ModulrPaymentRequest(
        String sourceAccountId,
        BigDecimal amount,
        String currency,
        String reference,
        String externalReference,
        ModulrDestination destination,
        String permittedScheme
) {

    /**
     * Beneficiary destination for IBAN-based SEPA transfers.
     *
     * @param type destination type (e.g., "IBAN")
     * @param iban beneficiary IBAN
     * @param name beneficiary name
     */
    record ModulrDestination(
            String type,
            String iban,
            String name
    ) {}
}
