package com.stablecoin.payments.merchant.iam.domain.team.model.core;

import java.util.List;

import static com.stablecoin.payments.merchant.iam.domain.team.model.core.Permission.of;

public enum BuiltInRole {

    ADMIN("Administrator", List.of(
            of("*", "*")
    )),

    PAYMENTS_OPERATOR("Payments Operator", List.of(
            of("payments", "*"),
            of("transactions", "*"),
            of("exports", "read"),
            of("team", "read")
    )),

    VIEWER("Viewer", List.of(
            of("payments", "read"),
            of("transactions", "read"),
            of("transactions", "export"),
            of("exports", "read"),
            of("settings", "read"),
            of("team", "read"),
            of("roles", "read")
    )),

    DEVELOPER("Developer", List.of(
            of("payments", "read"),
            of("transactions", "read"),
            of("webhooks", "*"),
            of("api_keys", "*")
    ));

    private final String description;
    private final List<Permission> defaultPermissions;

    BuiltInRole(String description, List<Permission> defaultPermissions) {
        this.description = description;
        this.defaultPermissions = List.copyOf(defaultPermissions);
    }

    public String description() {
        return description;
    }

    public List<Permission> defaultPermissions() {
        return defaultPermissions;
    }
}
