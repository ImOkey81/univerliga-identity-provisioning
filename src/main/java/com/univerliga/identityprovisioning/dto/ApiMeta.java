package com.univerliga.identityprovisioning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record ApiMeta(
    @Schema(example = "c1f54f65-37bc-4115-8c3b-33270a1159e3")
    String requestId,
    @Schema(example = "2026-02-28T15:01:02Z")
    OffsetDateTime timestamp,
    @Schema(example = "v1")
    String version
) {
}
