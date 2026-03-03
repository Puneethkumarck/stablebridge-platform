package com.stablecoin.payments.merchant.iam.infrastructure.persistence.adapter;

import com.stablecoin.payments.merchant.iam.domain.team.InvitationRepository;
import com.stablecoin.payments.merchant.iam.domain.team.model.Invitation;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.mapper.InvitationEntityMapper;
import com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository.InvitationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class InvitationRepositoryAdapter implements InvitationRepository {

    private final InvitationJpaRepository jpa;
    private final InvitationEntityMapper mapper;

    @Override
    public Optional<Invitation> findById(UUID invitationId) {
        return jpa.findById(invitationId).map(mapper::toDomain);
    }

    @Override
    public Optional<Invitation> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public List<Invitation> findByMerchantId(UUID merchantId) {
        return jpa.findByMerchantIdAndStatus(merchantId, "PENDING").stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Invitation save(Invitation invitation) {
        var existing = jpa.findById(invitation.invitationId());
        if (existing.isPresent()) {
            var entity = existing.get();
            entity.setStatus(invitation.status().name());
            entity.setAcceptedAt(invitation.acceptedAt());
            entity.setExpiresAt(invitation.expiresAt());
            return mapper.toDomain(jpa.save(entity));
        }
        return mapper.toDomain(jpa.save(mapper.toEntity(invitation)));
    }
}
