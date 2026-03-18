package com.stablecoin.payments.compliance.infrastructure.provider.ofacsdn;

import java.util.List;

record SdnEntry(
        int uid,
        String firstName,
        String lastName,
        String sdnType,
        List<String> programs,
        List<SdnAlias> aliases
) {
    SdnEntry {
        if (programs == null) {
            programs = List.of();
        }
        if (aliases == null) {
            aliases = List.of();
        }
    }

    String fullName() {
        if (firstName == null || firstName.isBlank()) {
            return lastName != null ? lastName.trim() : "";
        }
        return (firstName.trim() + " " + lastName.trim()).trim();
    }

    List<String> allNames() {
        var names = new java.util.ArrayList<String>();
        var primary = fullName();
        if (!primary.isBlank()) {
            names.add(primary);
        }
        for (var alias : aliases) {
            var aliasName = alias.fullName();
            if (!aliasName.isBlank()) {
                names.add(aliasName);
            }
        }
        return List.copyOf(names);
    }
}
