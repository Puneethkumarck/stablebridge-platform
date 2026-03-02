package com.stablecoin.payments.merchant.onboarding.application.controller;

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
import com.stablecoin.payments.merchant.onboarding.application.service.MerchantApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantApplicationService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('merchant:write')")
    public MerchantApplicationResponse apply(@Valid @RequestBody MerchantApplicationRequest request) {
        return service.apply(request);
    }

    @GetMapping("/{merchantId}")
    @PreAuthorize("hasAuthority('merchant:read')")
    public MerchantResponse findById(@PathVariable UUID merchantId) {
        return service.findById(merchantId);
    }

    @PatchMapping("/{merchantId}")
    @PreAuthorize("hasAuthority('merchant:write')")
    public MerchantResponse updateMerchant(
            @PathVariable UUID merchantId,
            @Valid @RequestBody UpdateMerchantRequest request) {
        return service.updateMerchant(merchantId, request);
    }

    @PostMapping("/{merchantId}/kyb/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('admin')")
    public void startKyb(@PathVariable UUID merchantId) {
        service.startKyb(merchantId);
    }

    @GetMapping("/{merchantId}/kyb")
    @PreAuthorize("hasAuthority('merchant:read')")
    public KybStatusResponse getKybStatus(@PathVariable UUID merchantId) {
        return service.getKybStatus(merchantId);
    }

    @PostMapping("/{merchantId}/activate")
    @PreAuthorize("hasAuthority('admin')")
    public MerchantResponse activate(
            @PathVariable UUID merchantId,
            @Valid @RequestBody ActivateMerchantRequest request) {
        return service.activate(merchantId, request);
    }

    @PostMapping("/{merchantId}/suspend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('admin')")
    public void suspend(
            @PathVariable UUID merchantId,
            @Valid @RequestBody SuspendMerchantRequest request) {
        service.suspend(merchantId, request);
    }

    @PostMapping("/{merchantId}/reactivate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('admin')")
    public void reactivate(@PathVariable UUID merchantId) {
        service.reactivate(merchantId);
    }

    @PostMapping("/{merchantId}/close")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('admin')")
    public void close(
            @PathVariable UUID merchantId,
            @RequestBody(required = false) CloseMerchantRequest request) {
        service.close(merchantId, request);
    }

    @PostMapping("/{merchantId}/corridors")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('admin')")
    public CorridorResponse approveCorridor(
            @PathVariable UUID merchantId,
            @Valid @RequestBody ApproveCorridorRequest request,
            @RequestHeader("X-Approved-By") UUID approvedBy) {
        return service.approveCorridor(merchantId, request, approvedBy);
    }

    @PostMapping("/{merchantId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('merchant:write')")
    public DocumentUploadResponse uploadDocument(
            @PathVariable UUID merchantId,
            @Valid @RequestBody DocumentUploadRequest request) {
        return service.uploadDocument(merchantId, request);
    }

    @PatchMapping("/{merchantId}/rate-limit-tier")
    @PreAuthorize("hasAuthority('admin')")
    public MerchantResponse updateRateLimitTier(
            @PathVariable UUID merchantId,
            @Valid @RequestBody UpdateRateLimitTierRequest request) {
        return service.updateRateLimitTier(merchantId, request);
    }
}
