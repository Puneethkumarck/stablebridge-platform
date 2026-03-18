package com.stablecoin.payments.compliance.infrastructure.provider.ofacsdn;

record SdnMatchResult(
        int entryUid,
        String matchedName,
        String sdnType,
        double score
) {}
