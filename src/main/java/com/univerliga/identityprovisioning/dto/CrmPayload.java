package com.univerliga.identityprovisioning.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record CrmPayload(
    @NotBlank String personId,
    @NotBlank String username,
    @NotBlank @Email String email,
    @NotBlank String displayName,
    @NotEmpty Set<String> roles,
    Boolean enabled
) {
}
