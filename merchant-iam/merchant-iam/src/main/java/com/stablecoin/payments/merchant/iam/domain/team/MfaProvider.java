package com.stablecoin.payments.merchant.iam.domain.team;

public interface MfaProvider {

    String generateSecret();

    String generateProvisioningUri(String email, String secret);

    boolean verify(String secret, String totpCode);
}
