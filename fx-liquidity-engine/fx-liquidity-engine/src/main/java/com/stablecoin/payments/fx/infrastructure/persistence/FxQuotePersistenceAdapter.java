package com.stablecoin.payments.fx.infrastructure.persistence;

import com.stablecoin.payments.fx.domain.model.FxQuote;
import com.stablecoin.payments.fx.domain.port.FxQuoteRepository;
import com.stablecoin.payments.fx.infrastructure.persistence.entity.FxQuoteJpaRepository;
import com.stablecoin.payments.fx.infrastructure.persistence.mapper.FxQuoteEntityUpdater;
import com.stablecoin.payments.fx.infrastructure.persistence.mapper.FxQuotePersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FxQuotePersistenceAdapter implements FxQuoteRepository {

    private final FxQuoteJpaRepository jpa;
    private final FxQuotePersistenceMapper mapper;
    private final FxQuoteEntityUpdater updater;

    @Override
    public FxQuote save(FxQuote quote) {
        var existing = jpa.findById(quote.quoteId());
        if (existing.isPresent()) {
            updater.updateEntity(existing.get(), quote);
            return mapper.toDomain(jpa.save(existing.get()));
        }
        return mapper.toDomain(jpa.save(mapper.toEntity(quote)));
    }

    @Override
    public Optional<FxQuote> findById(UUID quoteId) {
        return jpa.findById(quoteId).map(mapper::toDomain);
    }
}
