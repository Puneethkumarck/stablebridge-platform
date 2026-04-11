package com.stablecoin.payments.merchant.iam.client;

import com.stablecoin.payments.merchant.iam.api.response.DataResponse;
import com.stablecoin.payments.merchant.iam.api.response.PageResponse;
import com.stablecoin.payments.merchant.iam.api.response.PermissionCheckResponse;
import com.stablecoin.payments.merchant.iam.api.response.RoleResponse;
import com.stablecoin.payments.merchant.iam.api.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "merchant-iam", url = "${clients.merchant-iam.url}")
public interface MerchantIamClient {

    @GetMapping("/v1/auth/permissions/check")
    DataResponse<PermissionCheckResponse> checkPermission(
            @RequestParam("user_id") UUID userId,
            @RequestParam("merchant_id") UUID merchantId,
            @RequestParam("permission") String permission,
            @RequestHeader("Authorization") String bearerToken);

    @GetMapping("/v1/merchants/{merchantId}/users")
    PageResponse<UserResponse> listUsers(
            @PathVariable UUID merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestHeader("Authorization") String bearerToken);

    @GetMapping("/v1/merchants/{merchantId}/roles")
    PageResponse<RoleResponse> listRoles(
            @PathVariable UUID merchantId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestHeader("Authorization") String bearerToken);

    @GetMapping("/v1/.well-known/jwks.json")
    String jwks();
}
