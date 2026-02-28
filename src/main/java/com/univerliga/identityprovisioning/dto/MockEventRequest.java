package com.univerliga.identityprovisioning.dto;

import com.univerliga.identityprovisioning.domain.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MockEventRequest(
    @Schema(example = "11111111-1111-1111-1111-111111111111")
    @NotNull
    UUID eventId,
    @Schema(example = "PersonCreated")
    @NotNull
    EventType type,
    @Schema(example = "2026-02-28T12:00:00Z")
    @NotNull
    OffsetDateTime occurredAt,
    @Schema(example = "crm-service")
    @NotBlank
    String source,
    @Schema(description = "CRM payload")
    @NotNull
    @Valid
    CrmPayload payload
) {
}
