package com.stablecoin.payments.compliance.infrastructure.provider.persona;

import java.util.Map;

record PersonaInquiryResponse(PersonaInquiryData data) {

    record PersonaInquiryData(String id, String type, Map<String, Object> attributes) {

        String status() {
            if (attributes == null) {
                return null;
            }
            var value = attributes.get("status");
            return value != null ? value.toString() : null;
        }
    }
}
