package com.stablecoin.payments.merchant.iam.domain.exceptions;

import java.util.UUID;

public class BuiltInRoleModificationException extends RuntimeException {

    private BuiltInRoleModificationException(String message) {
        super(message);
    }

    public static BuiltInRoleModificationException forRole(UUID roleId, String roleName) {
        return new BuiltInRoleModificationException(
                "Cannot modify built-in role: %s (%s)".formatted(roleName, roleId));
    }
}
