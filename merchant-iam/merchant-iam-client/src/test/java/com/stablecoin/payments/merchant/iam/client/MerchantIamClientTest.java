package com.stablecoin.payments.merchant.iam.client;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantIamClientTest {

    @Test
    void client_has_feign_annotation() {
        var annotation = MerchantIamClient.class.getAnnotation(FeignClient.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("merchant-iam");
        assertThat(annotation.url()).isEqualTo("${clients.merchant-iam.url}");
    }

    @Test
    void check_permission_method_exists_with_correct_mapping() throws Exception {
        var method = MerchantIamClient.class.getMethod(
                "checkPermission", UUID.class, UUID.class, String.class, String.class);

        var getMapping = method.getAnnotation(GetMapping.class);
        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).contains("/v1/auth/permissions/check");
    }

    @Test
    void check_permission_has_required_request_params() throws Exception {
        var method = MerchantIamClient.class.getMethod(
                "checkPermission", UUID.class, UUID.class, String.class, String.class);
        var params = method.getParameters();

        var paramAnnotations = Arrays.stream(params)
                .map(p -> p.getAnnotation(RequestParam.class))
                .filter(Objects::nonNull)
                .map(RequestParam::value)
                .toList();

        assertThat(paramAnnotations).containsExactlyInAnyOrder("user_id", "merchant_id", "permission");
    }

    @Test
    void check_permission_has_authorization_header_param() throws Exception {
        var method = MerchantIamClient.class.getMethod(
                "checkPermission", UUID.class, UUID.class, String.class, String.class);
        var params = method.getParameters();

        var hasAuthHeader = Arrays.stream(params)
                .anyMatch(p -> {
                    var header = p.getAnnotation(RequestHeader.class);
                    return header != null && "Authorization".equals(header.value());
                });

        assertThat(hasAuthHeader).isTrue();
    }

    @Test
    void jwks_method_has_no_auth_header() throws Exception {
        var method = MerchantIamClient.class.getMethod("jwks");
        var params = method.getParameters();

        var hasAuthHeader = Arrays.stream(params)
                .anyMatch(p -> p.getAnnotation(RequestHeader.class) != null);

        assertThat(hasAuthHeader).isFalse();
    }

    @Test
    void jwks_endpoint_mapping_is_correct() throws Exception {
        var method = MerchantIamClient.class.getMethod("jwks");
        var getMapping = method.getAnnotation(GetMapping.class);
        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).contains("/v1/.well-known/jwks.json");
    }

    @Test
    void all_expected_methods_exist() {
        var methodNames = Arrays.stream(MerchantIamClient.class.getMethods())
                .map(Method::getName)
                .toList();

        assertThat(methodNames).contains(
                "checkPermission",
                "listUsers",
                "listRoles",
                "jwks");
    }
}
