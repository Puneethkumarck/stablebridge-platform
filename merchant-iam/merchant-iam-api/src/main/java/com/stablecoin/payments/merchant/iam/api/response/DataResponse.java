package com.stablecoin.payments.merchant.iam.api.response;

/**
 * Single-item envelope matching the spec's {@code {"data": {...}}} shape.
 */
public record DataResponse<T>(T data) {

    public static <T> DataResponse<T> of(T data) {
        return new DataResponse<>(data);
    }
}
