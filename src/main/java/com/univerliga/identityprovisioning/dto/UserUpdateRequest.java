package com.univerliga.identityprovisioning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

public record UserUpdateRequest(
    @Pattern(regexp = "^[a-zA-Z0-9_.-]{3,64}$")
    @Schema(example = "user123")
    String username,

    @NotBlank
    @Email
    @Schema(example = "u123@example.com")
    String email,

    @NotBlank
    @Schema(example = "User 123")
    String displayName,

    @Schema(example = "d_1")
    String departmentId,

    @Schema(example = "t_1")
    String teamId,

    @NotEmpty
    @Schema(example = "[\"ROLE_EMPLOYEE\"]")
    Set<String> roles,

    @Schema(example = "true")
    Boolean enabled
) {
}
