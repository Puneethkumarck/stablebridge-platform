package com.stablecoin.payments.custody.domain.port;

public interface TransferMonitorProperties {

    int resubmitTimeoutS();

    int maxAttempts();

    default int confirmingTimeoutS() {
        return 300;
    }
}
