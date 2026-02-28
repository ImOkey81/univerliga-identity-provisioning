package com.univerliga.identityprovisioning.dto;

import java.time.OffsetDateTime;

public record UserToggleResponse(String personId, boolean enabled, String status, OffsetDateTime updatedAt) {
}
