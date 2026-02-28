package com.univerliga.identityprovisioning.dto;

import java.time.OffsetDateTime;

public record ProcessedEventDto(
    String eventId,
    String type,
    String status,
    OffsetDateTime processedAt,
    String error
) {
}
