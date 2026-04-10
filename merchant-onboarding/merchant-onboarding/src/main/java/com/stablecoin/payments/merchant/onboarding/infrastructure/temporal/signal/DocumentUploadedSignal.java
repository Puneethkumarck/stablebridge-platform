package com.stablecoin.payments.merchant.onboarding.infrastructure.temporal.signal;

import java.io.Serializable;

public record DocumentUploadedSignal(String documentType, String fileName, String s3Key) implements Serializable {
}
