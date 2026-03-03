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

/**
 * Feign client for S13 Merchant IAM & Role Management.
 *
 * <p>Primary consumer: S10 API Gateway — calls {@code checkPermission} on every inbound request
 * to resolve whether the authenticated user has the required permission.
 *
 * <p>Configure the URL via {@code clients.merchant-iam.url} in the caller's {@code application.yml}.
 */
@FeignClient(name = "merchant-iam", url = "${clients.merchant-iam.url}")
public interface MerchantIamClient {

    /**
     * Checks whether a user has a specific permission.
     * Backed by Redis cache (60s TTL) with DB fallback.
     * Used by S10 for real-time per-request authorisation.
     *
     * @param userId      the user's UUID
     * @param merchantId  the merchant the user belongs to
     * @param permission  permission string, e.g. {@code payments:write} or {@code team:manage}
     * @param bearerToken the caller's service bearer token ({@code Authorization: Bearer <token>})
     */
    @GetMapping("/v1/auth/permissions/check")
    DataResponse<PermissionCheckResponse> checkPermission(
            @RequestParam("user_id") UUID userId,
            @RequestParam("merchant_id") UUID merchantId,
            @RequestParam("permission") String permission,
            @RequestHeader("Authorization") String bearerToken);

    /**
     * Lists all active users for a merchant (paginated).
     * Used by S10 to resolve user identity from a token.
     */
    @GetMapping("/v1/merchants/{merchantId}/users")
    PageResponse<UserResponse> listUsers(
            @PathVariable UUID merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestHeader("Authorization") String bearerToken);

    /**
     * Lists roles for a merchant.
     * Used to validate role assignments during token enrichment.
     */
    @GetMapping("/v1/merchants/{merchantId}/roles")
    PageResponse<RoleResponse> listRoles(
            @PathVariable UUID merchantId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestHeader("Authorization") String bearerToken);

    /**
     * Retrieves the JWKS (public key set) for JWT signature verification.
     * S10 fetches this once on startup and caches it.
     * No auth header required — public endpoint.
     */
    @GetMapping("/v1/.well-known/jwks.json")
    String jwks();
}
