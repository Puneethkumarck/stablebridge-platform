package com.stablecoin.payments.custody.domain.model;

public record NonceAssignment(
        Long nonce,
        NonceSource source
) {

    public enum NonceSource {
        INCREMENTED,
        REUSED,
        NOT_APPLICABLE
    }

    public static NonceAssignment notApplicable() {
        return new NonceAssignment(null, NonceSource.NOT_APPLICABLE);
    }

    public static NonceAssignment incremented(long nonce) {
        return new NonceAssignment(nonce, NonceSource.INCREMENTED);
    }

    public static NonceAssignment reused(long nonce) {
        return new NonceAssignment(nonce, NonceSource.REUSED);
    }
}
