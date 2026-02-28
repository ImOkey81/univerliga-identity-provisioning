package com.univerliga.identityprovisioning.dto;

import java.time.OffsetDateTime;

public record ReconcileResponse(String personId, String result, OffsetDateTime updatedAt) {
}
