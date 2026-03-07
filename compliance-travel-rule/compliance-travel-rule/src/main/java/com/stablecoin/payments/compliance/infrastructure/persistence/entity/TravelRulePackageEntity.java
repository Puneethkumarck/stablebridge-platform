package com.stablecoin.payments.compliance.infrastructure.persistence.entity;

import com.stablecoin.payments.compliance.domain.model.TransmissionStatus;
import com.stablecoin.payments.compliance.domain.model.TravelRuleProtocol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.util.UUID;

@Entity
@Table(name = "travel_rule_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelRulePackageEntity {

    @Id
    @Column(name = "package_id", updatable = false, nullable = false)
    private UUID packageId;

    @Column(name = "check_id", nullable = false)
    private UUID checkId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "originator_vasp", nullable = false, columnDefinition = "jsonb")
    private VaspInfoJson originatorVasp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "beneficiary_vasp", nullable = false, columnDefinition = "jsonb")
    private VaspInfoJson beneficiaryVasp;

    @Column(name = "originator_data", nullable = false, columnDefinition = "bytea")
    private byte[] originatorData;

    @Column(name = "beneficiary_data", nullable = false, columnDefinition = "bytea")
    private byte[] beneficiaryData;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", nullable = false, length = 20)
    private TravelRuleProtocol protocol;

    @Enumerated(EnumType.STRING)
    @Column(name = "transmission_status", nullable = false, length = 20)
    private TransmissionStatus transmissionStatus;

    @Column(name = "transmitted_at")
    private Instant transmittedAt;

    @Column(name = "protocol_ref", length = 200)
    private String protocolRef;

    public record VaspInfoJson(String vaspId, String name, String country, String did) {}
}
