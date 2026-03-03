package com.stablecoin.payments.merchant.iam.domain.team;

/**
 * Domain port for TOTP-based MFA (RFC 6238, Google Authenticator compatible).
 * Infrastructure provides a dev.samstevens.totp implementation.
 */
public interface MfaProvider {

    /**
     * Generates a new TOTP secret for a user.
     * @return Base32-encoded secret
     */
    String generateSecret();

    /**
     * Returns a provisioning URI for QR code generation.
     * @param email  user email (label in authenticator app)
     * @param secret Base32-encoded secret
     */
    String generateProvisioningUri(String email, String secret);

    /**
     * Verifies a 6-digit TOTP code against the secret.
     */
    boolean verify(String secret, String totpCode);
}
