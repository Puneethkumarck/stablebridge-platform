package com.stablecoin.payments.compliance.infrastructure.persistence.entity;

import com.stablecoin.payments.compliance.domain.model.KycTier;
import com.stablecoin.payments.compliance.domain.model.RiskBand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_risk_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRiskProfileEntity {

    @Id
    @Column(name = "customer_id", updatable = false, nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_tier", nullable = false, length = 20)
    private KycTier kycTier;

    @Column(name = "kyc_verified_at")
    private Instant kycVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_band", nullable = false, length = 10)
    private RiskBand riskBand;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "per_txn_limit_usd", nullable = false, precision = 20, scale = 2)
    private BigDecimal perTxnLimitUsd;

    @Column(name = "daily_limit_usd", nullable = false, precision = 20, scale = 2)
    private BigDecimal dailyLimitUsd;

    @Column(name = "monthly_limit_usd", nullable = false, precision = 20, scale = 2)
    private BigDecimal monthlyLimitUsd;

    @Column(name = "last_scored_at", nullable = false)
    private Instant lastScoredAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;
}
