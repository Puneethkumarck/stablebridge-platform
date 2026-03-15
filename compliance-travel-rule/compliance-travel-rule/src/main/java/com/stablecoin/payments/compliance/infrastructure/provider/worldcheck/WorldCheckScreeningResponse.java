package com.stablecoin.payments.compliance.infrastructure.provider.worldcheck;

import java.util.List;

record WorldCheckScreeningResponse(
        String caseId,
        String caseSystemId,
        String screeningState,
        List<MatchResult> results
) {
    record MatchResult(
            String referenceId,
            String matchStrength,
            String matchedTerm,
            String matchedNameType,
            String submittedTerm,
            List<String> sources,
            List<String> categories
    ) {}
}
