package com.stablecoin.payments.compliance.infrastructure.persistence.entity;

import com.stablecoin.payments.compliance.domain.model.ComplianceCheckStatus;
import com.stablecoin.payments.compliance.domain.model.OverallResult;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "compliance_checks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceCheckEntity {

    @Id
    @Column(name = "check_id", updatable = false, nullable = false)
    private UUID checkId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "source_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal sourceAmount;

    @Column(name = "source_currency", nullable = false, length = 3)
    private String sourceCurrency;

    @Column(name = "target_currency", nullable = false, length = 3)
    private String targetCurrency;

    @Column(name = "source_country", nullable = false, length = 2)
    private String sourceCountry;

    @Column(name = "target_country", nullable = false, length = 2)
    private String targetCountry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ComplianceCheckStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_result", length = 20)
    private OverallResult overallResult;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_band", length = 10)
    private RiskBand riskBand;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "risk_factors", columnDefinition = "text[]")
    private List<String> riskFactors;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Version
    @Column(name = "version")
    private Long version;
}
