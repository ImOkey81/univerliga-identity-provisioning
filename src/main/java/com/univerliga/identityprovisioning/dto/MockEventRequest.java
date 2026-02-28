package com.univerliga.identityprovisioning.dto;

import com.univerliga.identityprovisioning.domain.EventType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MockEventRequest(
    @NotNull
    UUID eventId,
    @NotNull
    EventType type,
    @NotNull
    OffsetDateTime occurredAt,
    @NotNull
    @Valid
    CrmPayload payload
) {
}
