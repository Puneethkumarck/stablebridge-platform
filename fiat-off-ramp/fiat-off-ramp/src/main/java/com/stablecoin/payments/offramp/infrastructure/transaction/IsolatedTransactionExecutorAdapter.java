package com.stablecoin.payments.offramp.infrastructure.transaction;

import com.stablecoin.payments.offramp.domain.port.IsolatedTransactionExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Component
public class IsolatedTransactionExecutorAdapter implements IsolatedTransactionExecutor {

    private final TransactionTemplate requiresNewTx;

    public IsolatedTransactionExecutorAdapter(PlatformTransactionManager transactionManager) {
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void executeInNewTransaction(Runnable action) {
        requiresNewTx.executeWithoutResult(status -> action.run());
    }
}
