package com.stablecoin.payments.gateway.iam.application.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

public class MerchantAuthentication extends AbstractAuthenticationToken {

    private final UUID merchantId;
    private final UUID clientId;
    private final List<String> scopes;
    private final AuthMethod authMethod;

    public MerchantAuthentication(UUID merchantId, UUID clientId, List<String> scopes,
                                  AuthMethod authMethod) {
        super(scopes.stream()
                .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                .toList());
        this.merchantId = merchantId;
        this.clientId = clientId;
        this.scopes = List.copyOf(scopes);
        this.authMethod = authMethod;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return merchantId;
    }

    public UUID merchantId() {
        return merchantId;
    }

    public UUID clientId() {
        return clientId;
    }

    public List<String> scopes() {
        return scopes;
    }

    public AuthMethod authMethod() {
        return authMethod;
    }

    public enum AuthMethod {
        JWT,
        API_KEY
    }
}
