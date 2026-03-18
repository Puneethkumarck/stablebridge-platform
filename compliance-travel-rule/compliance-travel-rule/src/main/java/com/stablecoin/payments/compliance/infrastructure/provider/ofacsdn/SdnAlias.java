package com.stablecoin.payments.compliance.infrastructure.provider.ofacsdn;

record SdnAlias(
        int uid,
        String type,
        String category,
        String firstName,
        String lastName
) {
    String fullName() {
        if (firstName == null || firstName.isBlank()) {
            return lastName != null ? lastName.trim() : "";
        }
        return (firstName.trim() + " " + lastName.trim()).trim();
    }
}
