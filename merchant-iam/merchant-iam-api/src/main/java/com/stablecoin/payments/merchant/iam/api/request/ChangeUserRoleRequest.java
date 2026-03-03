package com.stablecoin.payments.merchant.iam.api.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeUserRoleRequest(@NotNull UUID roleId) {}
