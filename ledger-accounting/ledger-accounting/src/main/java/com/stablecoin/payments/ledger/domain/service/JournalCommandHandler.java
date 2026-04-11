package com.stablecoin.payments.ledger.domain.service;

import com.stablecoin.payments.ledger.domain.exception.DuplicateTransactionException;
import com.stablecoin.payments.ledger.domain.model.AccountBalance;
import com.stablecoin.payments.ledger.domain.model.AuditEvent;
import com.stablecoin.payments.ledger.domain.model.JournalEntry;
import com.stablecoin.payments.ledger.domain.model.LedgerTransaction;
import com.stablecoin.payments.ledger.domain.port.AccountBalanceRepository;
import com.stablecoin.payments.ledger.domain.port.AuditEventRepository;
import com.stablecoin.payments.ledger.domain.port.JournalEntryRepository;
import com.stablecoin.payments.ledger.domain.port.LedgerTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class JournalCommandHandler {

    private static final String SERVICE_NAME = "ledger-accounting";
    private static final String JOURNAL_POSTED_EVENT = "journal.posted";
    private static final String SYSTEM_ACTOR = "system";

    private final LedgerTransactionRepository transactionRepository;
    private final JournalEntryRepository entryRepository;
    private final AccountBalanceRepository balanceRepository;
    private final AuditEventRepository auditEventRepository;
    private final BalanceCalculator balanceCalculator;
    private final Clock clock;

    public LedgerTransaction postTransaction(TransactionRequest request) {
        if (transactionRepository.existsBySourceEventId(request.sourceEventId())) {
            return findExistingTransaction(request);
        }

        var now = clock.instant();
        var transactionId = UUID.randomUUID();
        var baseSequence = entryRepository.countByPaymentId(request.paymentId());

        var balanceUpdates = balanceCalculator.computeBalances(request.entries());

        var entries = buildEntries(request, transactionId, baseSequence, balanceUpdates, now);

        var transaction = new LedgerTransaction(
                transactionId, request.paymentId(), request.correlationId(),
                request.sourceEvent(), request.sourceEventId(), request.description(),
                entries, now
        );

        var saved = transactionRepository.save(transaction);

        persistBalanceUpdates(entries, balanceUpdates, now);

        saveAuditEvent(request, transactionId, entries.size(), now);

        return saved;
    }

    private LedgerTransaction findExistingTransaction(TransactionRequest request) {
        return transactionRepository.findByPaymentId(request.paymentId()).stream()
                .filter(t -> t.sourceEventId().equals(request.sourceEventId()))
                .findFirst()
                .orElseThrow(() -> new DuplicateTransactionException(request.sourceEventId()));
    }

    private List<JournalEntry> buildEntries(
            TransactionRequest request,
            UUID transactionId,
            int baseSequence,
            Map<String, BalanceUpdate> balanceUpdates,
            Instant now
    ) {
        var entries = new ArrayList<JournalEntry>();
        for (var i = 0; i < request.entries().size(); i++) {
            var req = request.entries().get(i);
            var key = BalanceCalculator.balanceKey(req.accountCode(), req.currency());
            var update = balanceUpdates.get(key);

            entries.add(new JournalEntry(
                    UUID.randomUUID(),
                    transactionId,
                    request.paymentId(),
                    request.correlationId(),
                    baseSequence + i + 1,
                    req.entryType(),
                    req.accountCode(),
                    req.amount(),
                    req.currency(),
                    update.balanceAfter(),
                    update.accountVersion(),
                    request.sourceEvent(),
                    request.sourceEventId(),
                    now
            ));
        }
        return entries;
    }

    private void persistBalanceUpdates(
            List<JournalEntry> entries,
            Map<String, BalanceUpdate> updates,
            Instant now
    ) {
        var persisted = new HashSet<String>();
        for (var entry : entries) {
            var key = BalanceCalculator.balanceKey(entry.accountCode(), entry.currency());
            if (persisted.add(key)) {
                var update = updates.get(key);
                balanceRepository.save(new AccountBalance(
                        entry.accountCode(),
                        entry.currency(),
                        update.balanceAfter(),
                        update.accountVersion(),
                        entry.entryId(),
                        now
                ));
            }
        }
    }

    private void saveAuditEvent(
            TransactionRequest request,
            UUID transactionId,
            int entryCount,
            Instant now
    ) {
        var payload = "{\"transactionId\":\"" + transactionId
                + "\",\"sourceEvent\":\"" + request.sourceEvent()
                + "\",\"entryCount\":" + entryCount + "}";

        auditEventRepository.save(new AuditEvent(
                UUID.randomUUID(),
                request.correlationId(),
                request.paymentId(),
                SERVICE_NAME,
                JOURNAL_POSTED_EVENT,
                payload,
                SYSTEM_ACTOR,
                now,
                now
        ));
    }
}
