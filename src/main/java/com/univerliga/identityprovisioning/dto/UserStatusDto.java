package com.univerliga.identityprovisioning.dto;

import java.time.OffsetDateTime;
import java.util.Set;

public record UserStatusDto(
    String personId,
    String keycloakUserId,
    String status,
    String username,
    String email,
    Set<String> roles,
    boolean enabled,
    String lastError,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
