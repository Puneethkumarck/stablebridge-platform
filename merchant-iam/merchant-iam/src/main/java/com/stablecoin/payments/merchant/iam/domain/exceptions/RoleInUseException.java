package com.stablecoin.payments.merchant.iam.domain.exceptions;

import java.util.UUID;

public class RoleInUseException extends RuntimeException {

    private RoleInUseException(String message) {
        super(message);
    }

    public static RoleInUseException forRole(UUID roleId, long activeUserCount) {
        return new RoleInUseException(
                "Cannot delete role %s: %d active users assigned".formatted(roleId, activeUserCount));
    }
}
