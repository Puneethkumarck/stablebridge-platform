package com.stablecoin.payments.custody.domain.port;

import com.stablecoin.payments.custody.domain.model.ChainSelectionResult;

import java.util.UUID;

public interface ChainSelectionLogRepository {

    void save(UUID transferId, ChainSelectionResult result);
}
