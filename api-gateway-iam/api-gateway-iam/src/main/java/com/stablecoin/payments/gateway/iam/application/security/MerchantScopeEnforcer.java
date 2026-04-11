package com.stablecoin.payments.gateway.iam.application.security;

import com.stablecoin.payments.gateway.iam.domain.exception.ApiKeyNotFoundException;
import com.stablecoin.payments.gateway.iam.domain.exception.MerchantAccessDeniedException;
import com.stablecoin.payments.gateway.iam.domain.exception.TokenRevokedException;
import com.stablecoin.payments.gateway.iam.domain.port.AccessTokenRepository;
import com.stablecoin.payments.gateway.iam.domain.port.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MerchantScopeEnforcer {

    private final ApiKeyRepository apiKeyRepository;
    private final AccessTokenRepository accessTokenRepository;

    public boolean hasAccess(UUID targetMerchantId) {
        var principalMerchantId = authenticatedMerchantId();
        if (!principalMerchantId.equals(targetMerchantId)) {
            throw MerchantAccessDeniedException.forMerchant(targetMerchantId);
        }
        return true;
    }

    public boolean hasAccessToApiKey(UUID keyId) {
        var apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> ApiKeyNotFoundException.byId(keyId));
        return hasAccess(apiKey.getMerchantId());
    }

    public boolean hasAccessToToken(UUID jti) {
        var token = accessTokenRepository.findByJti(jti)
                .orElseThrow(() -> TokenRevokedException.of(jti));
        return hasAccess(token.getMerchantId());
    }

    public UUID authenticatedMerchantId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return extractMerchantId(auth);
    }

    private UUID extractMerchantId(Authentication auth) {
        if (auth instanceof MerchantAuthentication merchant) {
            return merchant.merchantId();
        }
        if (auth instanceof UserAuthentication user) {
            return user.merchantId();
        }
        throw MerchantAccessDeniedException.forMerchant(null);
    }
}
