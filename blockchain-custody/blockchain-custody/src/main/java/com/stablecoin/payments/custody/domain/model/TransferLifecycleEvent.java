package com.stablecoin.payments.custody.domain.model;

import lombok.AccessLevel;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true, access = AccessLevel.PACKAGE)
public record TransferLifecycleEvent(
        UUID eventId,
        UUID transferId,
        String state,
        String participantType,
        String address,
        Instant occurredAt
) {

    // -- Factory Methods ------------------------------------------------

    public static TransferLifecycleEvent record(UUID transferId, String state) {
        if (transferId == null) {
            throw new IllegalArgumentException("transferId is required");
        }
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("state is required");
        }

        return TransferLifecycleEvent.builder()
                .eventId(UUID.randomUUID())
                .transferId(transferId)
                .state(state)
                .occurredAt(Instant.now())
                .build();
    }

    public static TransferLifecycleEvent record(UUID transferId, String state,
                                                String participantType, String address) {
        if (transferId == null) {
            throw new IllegalArgumentException("transferId is required");
        }
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("state is required");
        }

        return TransferLifecycleEvent.builder()
                .eventId(UUID.randomUUID())
                .transferId(transferId)
                .state(state)
                .participantType(participantType)
                .address(address)
                .occurredAt(Instant.now())
                .build();
    }
}
