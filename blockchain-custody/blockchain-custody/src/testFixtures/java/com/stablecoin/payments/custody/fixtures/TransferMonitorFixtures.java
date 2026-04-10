package com.stablecoin.payments.custody.fixtures;

import com.stablecoin.payments.custody.domain.model.ChainId;
import com.stablecoin.payments.custody.domain.model.ChainTransfer;
import com.stablecoin.payments.custody.domain.model.StablecoinTicker;
import com.stablecoin.payments.custody.domain.model.TransferType;
import com.stablecoin.payments.custody.domain.model.WalletBalance;
import com.stablecoin.payments.custody.domain.port.ChainConfirmationProperties;
import com.stablecoin.payments.custody.domain.port.SignResult;
import com.stablecoin.payments.custody.domain.port.TokenContractResolver;
import com.stablecoin.payments.custody.domain.port.TransactionReceipt;
import com.stablecoin.payments.custody.domain.port.TransferMonitorProperties;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public final class TransferMonitorFixtures {

    private TransferMonitorFixtures() {}

    public static final UUID PAYMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID FROM_WALLET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final String TO_ADDRESS = "0xRecipientAddress1234567890abcdef";
    public static final String FROM_ADDRESS = "0xSenderAddress1234567890abcdef";
    public static final String TX_HASH = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
    public static final String RESUBMIT_TX_HASH = "0xresubmit234567890abcdef1234567890abcdef1234567890abcdef1234567890";
    public static final ChainId CHAIN_BASE = new ChainId("base");
    public static final ChainId CHAIN_ETHEREUM = new ChainId("ethereum");
    public static final StablecoinTicker USDC = StablecoinTicker.of("USDC");
    public static final BigDecimal AMOUNT = new BigDecimal("1000.00");
    public static final long RECEIPT_BLOCK = 100L;
    public static final long LATEST_BLOCK = 110L;
    public static final BigDecimal GAS_USED = new BigDecimal("21000");
    public static final BigDecimal GAS_PRICE = new BigDecimal("25.5");
    public static final String USDC_BASE_CONTRACT = "0x036CbD53842c5426634e7929541eC2318f3dCF7e";

    private static final Map<String, Integer> CHAIN_CONFIRMATIONS = Map.of(
            "base", 1,
            "ethereum", 32,
            "solana", 1
    );

    public static TransferMonitorProperties defaultMonitorProperties() {
        return new TransferMonitorProperties() {
            @Override
            public int resubmitTimeoutS() {
                return 120;
            }

            @Override
            public int maxAttempts() {
                return 3;
            }

            @Override
            public int confirmingTimeoutS() {
                return 300;
            }
        };
    }

    public static ChainConfirmationProperties defaultChainConfirmationProperties() {
        return chainId -> {
            var confirmations = CHAIN_CONFIRMATIONS.get(chainId);
            if (confirmations == null) {
                throw new IllegalStateException("Unknown chain: " + chainId);
            }
            return confirmations;
        };
    }

    public static TokenContractResolver defaultTokenContractResolver() {
        return (chainId, stablecoin) -> USDC_BASE_CONTRACT;
    }

    public static ChainTransfer aSubmittedTransferOnBase() {
        return ChainTransfer.initiate(
                PAYMENT_ID, CORRELATION_ID, TransferType.FORWARD, null,
                USDC, AMOUNT, FROM_WALLET_ID, TO_ADDRESS, FROM_ADDRESS
        ).selectChain(CHAIN_BASE).startSigning(42L).submit(TX_HASH);
    }

    public static ChainTransfer aSubmittedTransferOnEthereum() {
        return ChainTransfer.initiate(
                PAYMENT_ID, CORRELATION_ID, TransferType.FORWARD, null,
                USDC, AMOUNT, FROM_WALLET_ID, TO_ADDRESS, FROM_ADDRESS
        ).selectChain(CHAIN_ETHEREUM).startSigning(42L).submit(TX_HASH);
    }

    public static ChainTransfer aConfirmingTransferOnBase() {
        return aSubmittedTransferOnBase().startConfirming();
    }

    public static ChainTransfer aResubmittingTransfer() {
        return aSubmittedTransferOnBase().markForResubmission();
    }

    public static ChainTransfer aMaxAttemptsResubmittingTransfer() {
        var transfer = aSubmittedTransferOnBase(); // attempt 1
        transfer = transfer.markForResubmission();
        transfer = transfer.resubmit("0xhash2"); // attempt 2
        transfer = transfer.markForResubmission();
        transfer = transfer.resubmit("0xhash3"); // attempt 3
        transfer = transfer.markForResubmission();
        return transfer;
    }

    public static TransactionReceipt aSuccessfulReceipt() {
        return new TransactionReceipt(TX_HASH, RECEIPT_BLOCK, true, GAS_USED, GAS_PRICE, 10);
    }

    public static TransactionReceipt aFailedReceipt() {
        return new TransactionReceipt(TX_HASH, RECEIPT_BLOCK, false, GAS_USED, GAS_PRICE, 10);
    }

    public static SignResult aResubmitSignResult() {
        return new SignResult(RESUBMIT_TX_HASH, "custody-tx-resubmit");
    }

    public static WalletBalance aBalanceWithReserved() {
        var balance = WalletBalance.initialize(FROM_WALLET_ID, CHAIN_BASE, USDC);
        var synced = balance.syncFromChain(new BigDecimal("5000.00"), 50L);
        return synced.reserve(AMOUNT);
    }
}
