package com.stablecoin.payments.merchant.iam.domain.team.model.core;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionSetTest {

    @Nested
    class Has {

        @Test
        void exact_match_returns_true() {
            var set =PermissionSet.of(List.of(
                    Permission.of("payments", "read"),
                    Permission.of("payments", "write")));

            assertThat(set.has(Permission.of("payments", "read"))).isTrue();
            assertThat(set.has(Permission.of("payments", "write"))).isTrue();
        }

        @Test
        void no_match_returns_false() {
            var set =PermissionSet.of(List.of(
                    Permission.of("payments", "read")));

            assertThat(set.has(Permission.of("team", "read"))).isFalse();
        }

        @Test
        void full_wildcard_matches_any_permission() {
            var set =PermissionSet.of(List.of(
                    Permission.of("*", "*")));

            assertThat(set.has(Permission.of("payments", "read"))).isTrue();
            assertThat(set.has(Permission.of("team", "manage"))).isTrue();
            assertThat(set.has(Permission.of("api_keys", "write"))).isTrue();
        }

        @Test
        void namespace_wildcard_matches_same_namespace() {
            var set =PermissionSet.of(List.of(
                    Permission.of("payments", "*")));

            assertThat(set.has(Permission.of("payments", "read"))).isTrue();
            assertThat(set.has(Permission.of("payments", "cancel"))).isTrue();
            assertThat(set.has(Permission.of("team", "read"))).isFalse();
        }

        @Test
        void empty_set_matches_nothing() {
            var set =PermissionSet.empty();

            assertThat(set.has(Permission.of("payments", "read"))).isFalse();
        }
    }

    @Nested
    class HasAll {

        @Test
        void returns_true_when_all_permissions_present() {
            var set =PermissionSet.of(List.of(
                    Permission.of("payments", "read"),
                    Permission.of("payments", "write"),
                    Permission.of("team", "read")));

            assertThat(set.hasAll(List.of(
                    Permission.of("payments", "read"),
                    Permission.of("team", "read")))).isTrue();
        }

        @Test
        void returns_false_when_one_permission_missing() {
            var set =PermissionSet.of(List.of(
                    Permission.of("payments", "read")));

            assertThat(set.hasAll(List.of(
                    Permission.of("payments", "read"),
                    Permission.of("team", "read")))).isFalse();
        }

        @Test
        void wildcard_satisfies_all() {
            var set =PermissionSet.of(List.of(
                    Permission.of("*", "*")));

            assertThat(set.hasAll(List.of(
                    Permission.of("payments", "read"),
                    Permission.of("team", "manage"),
                    Permission.of("roles", "write")))).isTrue();
        }
    }

    @Nested
    class HasAny {

        @Test
        void returns_true_when_at_least_one_matches() {
            var set =PermissionSet.of(List.of(
                    Permission.of("payments", "read")));

            assertThat(set.hasAny(List.of(
                    Permission.of("payments", "read"),
                    Permission.of("team", "manage")))).isTrue();
        }

        @Test
        void returns_false_when_none_match() {
            var set =PermissionSet.of(List.of(
                    Permission.of("payments", "read")));

            assertThat(set.hasAny(List.of(
                    Permission.of("team", "read"),
                    Permission.of("roles", "manage")))).isFalse();
        }
    }

    @Nested
    class BuiltInRolePermissions {

        @Test
        void admin_has_full_access() {
            var adminPerms = PermissionSet.of(BuiltInRole.ADMIN.defaultPermissions());

            assertThat(adminPerms.has(Permission.of("payments", "read"))).isTrue();
            assertThat(adminPerms.has(Permission.of("team", "manage"))).isTrue();
            assertThat(adminPerms.has(Permission.of("compliance", "read"))).isTrue();
            assertThat(adminPerms.has(Permission.of("exports", "read"))).isTrue();
        }

        @Test
        void payments_operator_has_payment_and_transaction_permissions() {
            var perms =PermissionSet.of(BuiltInRole.PAYMENTS_OPERATOR.defaultPermissions());

            // payments:* covers read/write/cancel
            assertThat(perms.has(Permission.of("payments", "read"))).isTrue();
            assertThat(perms.has(Permission.of("payments", "write"))).isTrue();
            assertThat(perms.has(Permission.of("payments", "cancel"))).isTrue();
            // transactions:* covers read/export
            assertThat(perms.has(Permission.of("transactions", "read"))).isTrue();
            assertThat(perms.has(Permission.of("transactions", "export"))).isTrue();
            // exports:read and team:read
            assertThat(perms.has(Permission.of("exports", "read"))).isTrue();
            assertThat(perms.has(Permission.of("team", "read"))).isTrue();
            // should NOT have
            assertThat(perms.has(Permission.of("team", "manage"))).isFalse();
            assertThat(perms.has(Permission.of("roles", "manage"))).isFalse();
            assertThat(perms.has(Permission.of("settings", "write"))).isFalse();
        }

        @Test
        void viewer_has_read_only_permissions() {
            var perms =PermissionSet.of(BuiltInRole.VIEWER.defaultPermissions());

            assertThat(perms.has(Permission.of("payments", "read"))).isTrue();
            assertThat(perms.has(Permission.of("transactions", "read"))).isTrue();
            assertThat(perms.has(Permission.of("transactions", "export"))).isTrue();
            assertThat(perms.has(Permission.of("exports", "read"))).isTrue();
            assertThat(perms.has(Permission.of("settings", "read"))).isTrue();
            assertThat(perms.has(Permission.of("team", "read"))).isTrue();
            assertThat(perms.has(Permission.of("roles", "read"))).isTrue();
            // should NOT have
            assertThat(perms.has(Permission.of("payments", "write"))).isFalse();
            assertThat(perms.has(Permission.of("team", "manage"))).isFalse();
            assertThat(perms.has(Permission.of("settings", "write"))).isFalse();
        }

        @Test
        void developer_has_api_and_webhook_permissions() {
            var perms =PermissionSet.of(BuiltInRole.DEVELOPER.defaultPermissions());

            // webhooks:* and api_keys:* cover read/write
            assertThat(perms.has(Permission.of("api_keys", "read"))).isTrue();
            assertThat(perms.has(Permission.of("api_keys", "write"))).isTrue();
            assertThat(perms.has(Permission.of("webhooks", "read"))).isTrue();
            assertThat(perms.has(Permission.of("webhooks", "write"))).isTrue();
            assertThat(perms.has(Permission.of("payments", "read"))).isTrue();
            assertThat(perms.has(Permission.of("transactions", "read"))).isTrue();
            // should NOT have
            assertThat(perms.has(Permission.of("payments", "write"))).isFalse();
            assertThat(perms.has(Permission.of("team", "manage"))).isFalse();
            assertThat(perms.has(Permission.of("exports", "read"))).isFalse();
        }
    }

    @Nested
    class Properties {

        @Test
        void size_returns_permission_count() {
            var set = PermissionSet.of(List.of(
                    Permission.of("payments", "read"),
                    Permission.of("exports", "read")));

            assertThat(set.size()).isEqualTo(2);
        }

        @Test
        void empty_set_has_zero_size() {
            assertThat(PermissionSet.empty().size()).isZero();
            assertThat(PermissionSet.empty().isEmpty()).isTrue();
        }

        @Test
        void permissions_returns_immutable_copy() {
            var set = PermissionSet.of(List.of(
                    Permission.of("payments", "read")));
            var permissions = set.permissions();

            assertThat(permissions).containsExactly(Permission.of("payments", "read"));
        }
    }
}
