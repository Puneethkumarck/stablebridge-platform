package com.stablecoin.payments.compliance.infrastructure.provider.notabene;

import java.util.List;

record NotabeneCreateTransferRequest(
        String transactionRef,
        String transactionAsset,
        String transactionAmount,
        String originatorVASPdid,
        String beneficiaryVASPdid,
        Originator originator,
        Beneficiary beneficiary,
        TransactionBlockchainInfo transactionBlockchainInfo
) {

    record Originator(
            List<OriginatorPerson> originatorPersons,
            List<String> accountNumber
    ) {}

    record OriginatorPerson(NaturalPerson naturalPerson) {}

    record Beneficiary(
            List<BeneficiaryPerson> beneficiaryPersons,
            List<String> accountNumber
    ) {}

    record BeneficiaryPerson(NaturalPerson naturalPerson) {}

    record NaturalPerson(
            List<Name> name,
            List<GeographicAddress> geographicAddress
    ) {}

    record Name(List<NameIdentifier> nameIdentifier) {}

    record NameIdentifier(
            String primaryIdentifier,
            String secondaryIdentifier
    ) {}

    record GeographicAddress(
            List<String> addressLine,
            String country
    ) {}

    record TransactionBlockchainInfo(
            String origin,
            String destination
    ) {}
}
