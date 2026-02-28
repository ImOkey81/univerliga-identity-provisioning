package com.univerliga.identityprovisioning.keycloak;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KeycloakUserRepresentation(
    String id,
    String username,
    String email,
    boolean enabled,
    String firstName,
    String lastName,
    Map<String, List<String>> attributes
) {
}
