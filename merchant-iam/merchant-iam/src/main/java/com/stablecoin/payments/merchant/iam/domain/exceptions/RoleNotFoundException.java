package com.stablecoin.payments.merchant.iam.domain.exceptions;

import java.util.UUID;

public class RoleNotFoundException extends RuntimeException {

    private RoleNotFoundException(String message) {
        super(message);
    }

    public static RoleNotFoundException withId(UUID roleId) {
        return new RoleNotFoundException("Role not found: " + roleId);
    }

    public static RoleNotFoundException withName(UUID merchantId, String roleName) {
        return new RoleNotFoundException(
                "Role not found: merchant=%s name=%s".formatted(merchantId, roleName));
    }
}
