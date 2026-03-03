package com.stablecoin.payments.merchant.iam.domain.team.model.core;

import java.util.Objects;
import java.util.Set;

public record Permission(String namespace, String action) {

    public static final String WILDCARD = "*";

    /**
     * Allowed permission namespaces per spec Section 2.
     * Wildcard is permitted for ADMIN roles only.
     */
    public static final Set<String> ALLOWED_NAMESPACES = Set.of(
            "*", "payments", "transactions", "webhooks", "api_keys",
            "team", "roles", "settings", "exports", "compliance"
    );

    public static final Set<String> ALLOWED_ACTIONS = Set.of(
            "*", "read", "write", "cancel", "manage", "export"
    );

    public Permission {
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(action, "action must not be null");
        if (namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (action.isBlank()) {
            throw new IllegalArgumentException("action must not be blank");
        }
        if (!ALLOWED_NAMESPACES.contains(namespace)) {
            throw new IllegalArgumentException(
                    "Invalid permission namespace '" + namespace + "'. Allowed: " + ALLOWED_NAMESPACES);
        }
        if (!ALLOWED_ACTIONS.contains(action)) {
            throw new IllegalArgumentException(
                    "Invalid permission action '" + action + "'. Allowed: " + ALLOWED_ACTIONS);
        }
    }

    public static Permission of(String namespace, String action) {
        return new Permission(namespace, action);
    }

    public static Permission parse(String permission) {
        Objects.requireNonNull(permission, "permission string must not be null");
        String[] parts = permission.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid permission format: " + permission);
        }
        return new Permission(parts[0], parts[1]);
    }

    public boolean implies(Permission other) {
        boolean namespaceMatch = WILDCARD.equals(namespace) || namespace.equals(other.namespace);
        boolean actionMatch = WILDCARD.equals(action) || action.equals(other.action);
        return namespaceMatch && actionMatch;
    }

    @Override
    public String toString() {
        return namespace + ":" + action;
    }
}
