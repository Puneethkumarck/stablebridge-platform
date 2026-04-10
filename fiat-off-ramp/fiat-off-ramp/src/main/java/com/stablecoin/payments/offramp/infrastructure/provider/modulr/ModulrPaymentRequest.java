package com.stablecoin.payments.offramp.infrastructure.provider.modulr;

import java.math.BigDecimal;

record ModulrPaymentRequest(
        String sourceAccountId,
        BigDecimal amount,
        String currency,
        String reference,
        String externalReference,
        ModulrDestination destination,
        String permittedScheme
) {

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
