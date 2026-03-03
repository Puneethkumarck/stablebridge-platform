package com.stablecoin.payments.merchant.iam.application.controller;

import com.stablecoin.payments.merchant.iam.api.response.DataResponse;
import com.stablecoin.payments.merchant.iam.api.response.PermissionCheckResponse;
import com.stablecoin.payments.merchant.iam.domain.team.PermissionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/auth/permissions")
@RequiredArgsConstructor
@Validated
public class PermissionsController {

    private final PermissionQueryService permissionQueryService;

    /**
     * GET /v1/auth/permissions/check?user_id=&merchant_id=&permission=payments:write
     * Called by S10 API Gateway on every inbound request.
     */
    @GetMapping("/check")
    public DataResponse<PermissionCheckResponse> checkPermission(
            @RequestParam("user_id") UUID userId,
            @RequestParam("merchant_id") UUID merchantId,
            @RequestParam("permission") String permission) {
        var result = permissionQueryService.check(userId, merchantId, permission);
        return DataResponse.of(new PermissionCheckResponse(result.allowed(), result.roleName(), result.via()));
    }
}
