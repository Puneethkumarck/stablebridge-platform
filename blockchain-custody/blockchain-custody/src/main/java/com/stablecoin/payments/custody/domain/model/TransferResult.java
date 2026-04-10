package com.stablecoin.payments.custody.domain.model;

public record TransferResult(ChainTransfer transfer, boolean created) {}
