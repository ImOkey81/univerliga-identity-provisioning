package com.univerliga.identityprovisioning.keycloak;

import java.util.Set;

public record KeycloakUserCommand(
    String personId,
    String username,
    String email,
    String displayName,
    Set<String> roles,
    boolean enabled
) {
}
