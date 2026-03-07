package com.stablecoin.payments.compliance.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sanctions_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanctionsResultEntity {

    @Id
    @Column(name = "sanctions_result_id", updatable = false, nullable = false)
    private UUID sanctionsResultId;

    @Column(name = "check_id", nullable = false)
    private UUID checkId;

    @Column(name = "sender_screened", nullable = false)
    private boolean senderScreened;

    @Column(name = "recipient_screened", nullable = false)
    private boolean recipientScreened;

    @Column(name = "sender_hit", nullable = false)
    private boolean senderHit;

    @Column(name = "recipient_hit", nullable = false)
    private boolean recipientHit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hit_details", columnDefinition = "jsonb")
    private String hitDetails;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "lists_checked", nullable = false, columnDefinition = "text[]")
    private List<String> listsChecked;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_ref", nullable = false, length = 200)
    private String providerRef;

    @Column(name = "screened_at", nullable = false)
    private Instant screenedAt;
}
