package com.stablecoin.payments.offramp.domain.port;

public interface IsolatedTransactionExecutor {

    void executeInNewTransaction(Runnable action);
}
