package com.stablecoin.payments.ledger.domain.service;

import com.stablecoin.payments.ledger.domain.exception.AccountNotFoundException;
import com.stablecoin.payments.ledger.domain.model.Account;
import com.stablecoin.payments.ledger.domain.model.AccountBalance;
import com.stablecoin.payments.ledger.domain.model.EntryType;
import com.stablecoin.payments.ledger.domain.port.AccountBalanceRepository;
import com.stablecoin.payments.ledger.domain.port.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BalanceCalculator {

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository balanceRepository;

    public Map<String, BalanceUpdate> computeBalances(List<JournalEntryRequest> entries) {
        List<JournalEntryRequest> sorted = entries.stream()
                .sorted(Comparator.comparing(JournalEntryRequest::accountCode)
                        .thenComparing(JournalEntryRequest::currency))
                .toList();

        Map<String, BalanceUpdate> result = new LinkedHashMap<>();
        for (JournalEntryRequest req : sorted) {
            String key = balanceKey(req.accountCode(), req.currency());
            BalanceUpdate previous = result.get(key);
            result.put(key, computeSingleBalance(req, previous));
        }
        return result;
    }

    private BalanceUpdate computeSingleBalance(JournalEntryRequest entryRequest, BalanceUpdate previous) {
        Account account = accountRepository.findByAccountCode(entryRequest.accountCode())
                .orElseThrow(() -> new AccountNotFoundException(entryRequest.accountCode()));

        BigDecimal currentBalance;
        long newVersion;
        if (previous != null) {
            currentBalance = previous.balanceAfter();
            newVersion = previous.accountVersion();
        } else {
            AccountBalance current = balanceRepository
                    .findForUpdate(entryRequest.accountCode(), entryRequest.currency())
                    .orElse(AccountBalance.zero(entryRequest.accountCode(), entryRequest.currency()));
            currentBalance = current.balance();
            newVersion = current.version() + 1;
        }

        BigDecimal newBalance = computeNewBalance(
                currentBalance, entryRequest.entryType(), account.normalBalance(), entryRequest.amount());

        return new BalanceUpdate(newBalance, newVersion);
    }

    static BigDecimal computeNewBalance(
            BigDecimal currentBalance,
            EntryType entryType,
            EntryType normalBalance,
            BigDecimal amount
    ) {
        if (entryType == normalBalance) {
            return currentBalance.add(amount);
        }
        return currentBalance.subtract(amount);
    }

    public static String balanceKey(String accountCode, String currency) {
        return accountCode + ":" + currency;
    }
}
