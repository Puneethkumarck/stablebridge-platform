package com.stablecoin.payments.compliance.infrastructure.provider.notabene;

record NotabeneTransferResponse(
        String id,
        String status,
        String transactionRef,
        String transactionAsset,
        String transactionAmount,
        String originatorVASPdid,
        String beneficiaryVASPdid,
        String createdAt,
        String updatedAt
) {}
