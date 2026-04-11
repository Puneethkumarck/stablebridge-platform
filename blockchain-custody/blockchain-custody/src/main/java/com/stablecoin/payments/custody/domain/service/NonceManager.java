package com.stablecoin.payments.custody.domain.service;

import com.stablecoin.payments.custody.domain.model.ChainId;
import com.stablecoin.payments.custody.domain.model.NonceAssignment;
import com.stablecoin.payments.custody.domain.port.NonceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NonceManager {

    private static final Set<String> NONCE_BASED_CHAINS =
            Set.of("ethereum", "base", "polygon", "avalanche", "tron");

    private final NonceRepository nonceRepository;

    public NonceAssignment assignNonce(UUID walletId, ChainId chainId, boolean isResubmit) {
        if (walletId == null) {
            throw new IllegalArgumentException("walletId is required");
        }
        if (chainId == null) {
            throw new IllegalArgumentException("chainId is required");
        }

        if (!isNonceBasedChain(chainId)) {
            log.debug("Chain {} does not use nonces -- returning NOT_APPLICABLE for wallet={}",
                    chainId.value(), walletId);
            return NonceAssignment.notApplicable();
        }

        if (isResubmit) {
            return handleResubmit(walletId, chainId);
        }
        return handleFreshNonce(walletId, chainId);
    }

    private NonceAssignment handleResubmit(UUID walletId, ChainId chainId) {
        var currentNonce = nonceRepository.getCurrentNonce(walletId, chainId);
        if (currentNonce.isEmpty()) {
            throw new IllegalStateException(
                    "No existing nonce found for wallet %s on chain %s -- cannot resubmit"
                            .formatted(walletId, chainId.value()));
        }
        // Resubmit reuses the last assigned nonce (current_nonce - 1)
        // because current_nonce in DB tracks the *next* nonce to use
        long nonceToReuse = currentNonce.get() - 1;
        log.info("Resubmit: reusing nonce={} for wallet={} on chain={}",
                nonceToReuse, walletId, chainId.value());
        return NonceAssignment.reused(nonceToReuse);
    }

    private NonceAssignment handleFreshNonce(UUID walletId, ChainId chainId) {
        long nonce = nonceRepository.assignNextNonce(walletId, chainId);
        log.info("Assigned fresh nonce={} for wallet={} on chain={}",
                nonce, walletId, chainId.value());
        return NonceAssignment.incremented(nonce);
    }

    private boolean isNonceBasedChain(ChainId chainId) {
        return NONCE_BASED_CHAINS.contains(chainId.value());
    }
}
