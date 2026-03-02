package com.stablecoin.payments.merchant.onboarding.client;

import com.stablecoin.payments.merchant.onboarding.api.request.ActivateMerchantRequest;
import com.stablecoin.payments.merchant.onboarding.api.request.ApproveCorridorRequest;
import com.stablecoin.payments.merchant.onboarding.api.request.CloseMerchantRequest;
import com.stablecoin.payments.merchant.onboarding.api.request.DocumentUploadRequest;
import com.stablecoin.payments.merchant.onboarding.api.request.MerchantApplicationRequest;
import com.stablecoin.payments.merchant.onboarding.api.request.SuspendMerchantRequest;
import com.stablecoin.payments.merchant.onboarding.api.request.UpdateMerchantRequest;
import com.stablecoin.payments.merchant.onboarding.api.request.UpdateRateLimitTierRequest;
import com.stablecoin.payments.merchant.onboarding.api.response.CorridorResponse;
import com.stablecoin.payments.merchant.onboarding.api.response.DocumentUploadResponse;
import com.stablecoin.payments.merchant.onboarding.api.response.KybStatusResponse;
import com.stablecoin.payments.merchant.onboarding.api.response.MerchantApplicationResponse;
import com.stablecoin.payments.merchant.onboarding.api.response.MerchantResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "merchant-onboarding", url = "${clients.merchant-onboarding.url}")
public interface MerchantOnboardingClient {

    @PostMapping("/api/v1/merchants")
    MerchantApplicationResponse apply(@RequestBody MerchantApplicationRequest request);

    @GetMapping("/api/v1/merchants/{merchantId}")
    MerchantResponse findById(@PathVariable UUID merchantId);

    @PatchMapping("/api/v1/merchants/{merchantId}")
    MerchantResponse updateMerchant(@PathVariable UUID merchantId, @RequestBody UpdateMerchantRequest request);

    @PostMapping("/api/v1/merchants/{merchantId}/kyb/start")
    void startKyb(@PathVariable UUID merchantId);

    @GetMapping("/api/v1/merchants/{merchantId}/kyb")
    KybStatusResponse getKybStatus(@PathVariable UUID merchantId);

    @PostMapping("/api/v1/merchants/{merchantId}/activate")
    MerchantResponse activate(@PathVariable UUID merchantId, @RequestBody ActivateMerchantRequest request);

    @PostMapping("/api/v1/merchants/{merchantId}/suspend")
    void suspend(@PathVariable UUID merchantId, @RequestBody SuspendMerchantRequest request);

    @PostMapping("/api/v1/merchants/{merchantId}/reactivate")
    void reactivate(@PathVariable UUID merchantId);

    @PostMapping("/api/v1/merchants/{merchantId}/close")
    void close(@PathVariable UUID merchantId, @RequestBody CloseMerchantRequest request);

    @PostMapping("/api/v1/merchants/{merchantId}/corridors")
    CorridorResponse approveCorridor(
            @PathVariable UUID merchantId,
            @RequestBody ApproveCorridorRequest request,
            @RequestHeader("X-Approved-By") UUID approvedBy);

    @PostMapping("/api/v1/merchants/{merchantId}/documents")
    DocumentUploadResponse uploadDocument(@PathVariable UUID merchantId, @RequestBody DocumentUploadRequest request);

    @PatchMapping("/api/v1/merchants/{merchantId}/rate-limit-tier")
    MerchantResponse updateRateLimitTier(@PathVariable UUID merchantId, @RequestBody UpdateRateLimitTierRequest request);
}
