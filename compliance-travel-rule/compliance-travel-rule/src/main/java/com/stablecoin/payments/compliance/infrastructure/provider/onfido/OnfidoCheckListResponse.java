package com.stablecoin.payments.compliance.infrastructure.provider.onfido;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

record OnfidoCheckListResponse(List<OnfidoCheck> checks) {

    record OnfidoCheck(
            String id,
            String status,
            String result,
            @JsonProperty("applicant_id") String applicantId,
            @JsonProperty("report_ids") List<String> reportIds
    ) {}
}
