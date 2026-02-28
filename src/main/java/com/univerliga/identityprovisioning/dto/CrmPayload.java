package com.univerliga.identityprovisioning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record CrmPayload(
    @Schema(example = "p_123")
    @NotBlank String personId,
    @Schema(example = "user123")
    @NotBlank String username,
    @Schema(example = "u123@example.com")
    @NotBlank @Email String email,
    @Schema(example = "User 123")
    @NotBlank String displayName,
    @Schema(example = "[\"ROLE_EMPLOYEE\"]")
    @NotEmpty Set<String> roles,
    @Schema(example = "true")
    Boolean enabled
) {
}
