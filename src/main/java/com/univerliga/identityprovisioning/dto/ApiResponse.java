package com.univerliga.identityprovisioning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ApiResponse<T>(
    @Schema(description = "Response payload")
    T data,
    ApiMeta meta
) {
}
