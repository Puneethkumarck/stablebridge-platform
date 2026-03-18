package com.stablecoin.payments.compliance.infrastructure.provider.persona;

import java.util.Map;

record PersonaCreateInquiryRequest(Map<String, Object> data) {

    static PersonaCreateInquiryRequest of(String templateId, String referenceId) {
        return new PersonaCreateInquiryRequest(
                Map.of("attributes", Map.of(
                        "inquiry-template-id", templateId,
                        "reference-id", referenceId
                ))
        );
    }
}
