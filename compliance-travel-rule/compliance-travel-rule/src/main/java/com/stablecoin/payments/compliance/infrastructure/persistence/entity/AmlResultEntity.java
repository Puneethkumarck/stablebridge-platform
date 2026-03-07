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
@Table(name = "aml_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmlResultEntity {

    @Id
    @Column(name = "aml_result_id", updatable = false, nullable = false)
    private UUID amlResultId;

    @Column(name = "check_id", nullable = false)
    private UUID checkId;

    @Column(name = "flagged", nullable = false)
    private boolean flagged;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "flag_reasons", columnDefinition = "text[]")
    private List<String> flagReasons;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chain_analysis", columnDefinition = "jsonb")
    private String chainAnalysis;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_ref", length = 200)
    private String providerRef;

    @Column(name = "screened_at", nullable = false)
    private Instant screenedAt;
}
