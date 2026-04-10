package com.stablecoin.payments.custody.domain.port;

public interface IsolatedTransactionExecutor {

    void executeInNewTransaction(Runnable action);
}
