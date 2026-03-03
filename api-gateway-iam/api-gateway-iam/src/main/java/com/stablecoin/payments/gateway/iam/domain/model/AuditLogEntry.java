package com.stablecoin.payments.gateway.iam.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class AuditLogEntry {

    private final UUID logId;
    private final UUID merchantId;
    private final String action;
    private final String resource;
    private final String sourceIp;
    private final Map<String, Object> detail;
    private final Instant occurredAt;
}
