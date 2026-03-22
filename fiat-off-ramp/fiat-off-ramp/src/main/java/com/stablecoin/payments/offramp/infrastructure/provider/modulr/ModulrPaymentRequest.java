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
 * @param currency         ISO 4217 currency code (e.g., EUR, GBP)
 * @param reference        payment reference visible to beneficiary
 * @param externalReference idempotency key / external correlation ID
 * @param destination      beneficiary destination details
 * @param permittedScheme  payment scheme (e.g., SEPA_CREDIT, FPS)
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
     * Beneficiary destination — supports IBAN (SEPA) and SCAN (FPS) types.
     * Null fields are excluded from JSON serialization.
     *
     * @param type          destination type: "IBAN" or "SCAN"
     * @param iban          beneficiary IBAN (SEPA only)
     * @param name          beneficiary name
     * @param sortCode      sort code (FPS/SCAN only)
     * @param accountNumber account number (FPS/SCAN only)
     */
    record ModulrDestination(
            String type,
            String iban,
            String name,
            String sortCode,
            String accountNumber
    ) {
        static ModulrDestination iban(String iban, String name) {
            return new ModulrDestination("IBAN", iban, name, null, null);
        }

        static ModulrDestination scan(String sortCode, String accountNumber, String name) {
            return new ModulrDestination("SCAN", null, name, sortCode, accountNumber);
        }
    }
}
