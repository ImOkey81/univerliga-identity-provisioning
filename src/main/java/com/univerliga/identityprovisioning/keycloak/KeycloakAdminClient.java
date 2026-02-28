package com.univerliga.identityprovisioning.keycloak;

import com.univerliga.identityprovisioning.config.AppProperties;
import com.univerliga.identityprovisioning.exception.BadGatewayException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class KeycloakAdminClient {

    private final RestClient keycloakRestClient;
    private final AppProperties properties;
    private final AtomicReference<TokenHolder> tokenCache = new AtomicReference<>();

    public KeycloakProvisionedUser createOrUpdate(KeycloakUserCommand command) {
        String token = accessToken();
        List<KeycloakUserRepresentation> byUsername = findByUsername(command.username(), token);
        KeycloakUserRepresentation existing = byUsername.stream().findFirst().orElse(null);

        boolean changed;
        String userId;

        if (existing == null) {
            userId = createUser(command, token);
            changed = true;
        } else {
            userId = existing.id();
            changed = updateUser(userId, command, token);
        }

        boolean rolesChanged = syncRoles(userId, command.roles(), token);
        return new KeycloakProvisionedUser(userId, changed || rolesChanged);
    }

    public void setEnabled(String userId, boolean enabled) {
        String token = accessToken();
        KeycloakUserRepresentation current = getUser(userId, token);
        KeycloakUserRepresentation update = new KeycloakUserRepresentation(
            null,
            current.username(),
            current.email(),
            enabled,
            current.firstName(),
            current.lastName(),
            current.attributes()
        );

        requestPut("/admin/realms/%s/users/%s".formatted(properties.getKeycloak().getRealm(), userId), update, token);
    }

    public KeycloakProvisionedUser reconcile(KeycloakUserCommand command, String userId) {
        String token = accessToken();
        boolean userChanged = updateUser(userId, command, token);
        boolean rolesChanged = syncRoles(userId, command.roles(), token);
        return new KeycloakProvisionedUser(userId, userChanged || rolesChanged);
    }

    private List<KeycloakUserRepresentation> findByUsername(String username, String token) {
        try {
            KeycloakUserRepresentation[] response = keycloakRestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/admin/realms/{realm}/users")
                    .queryParam("username", username)
                    .build(properties.getKeycloak().getRealm()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(KeycloakUserRepresentation[].class);
            return response == null ? Collections.emptyList() : Arrays.asList(response);
        } catch (RestClientException ex) {
            throw new BadGatewayException("Keycloak search user failed: " + ex.getMessage());
        }
    }

    private KeycloakUserRepresentation getUser(String userId, String token) {
        try {
            return keycloakRestClient.get()
                .uri("/admin/realms/%s/users/%s".formatted(properties.getKeycloak().getRealm(), userId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(KeycloakUserRepresentation.class);
        } catch (RestClientException ex) {
            throw new BadGatewayException("Keycloak get user failed: " + ex.getMessage());
        }
    }

    private String createUser(KeycloakUserCommand command, String token) {
        KeycloakUserRepresentation user = mapUser(command, null);
        try {
            var response = keycloakRestClient.post()
                .uri("/admin/realms/%s/users".formatted(properties.getKeycloak().getRealm()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(user)
                .retrieve()
                .toBodilessEntity()
                .getHeaders();

            if (response.getLocation() != null) {
                return response.getLocation().getPath().replaceAll("^.*/", "");
            }

            return findByUsername(command.username(), token).stream()
                .findFirst()
                .map(KeycloakUserRepresentation::id)
                .orElseThrow(() -> new BadGatewayException("Keycloak create user did not return user id"));
        } catch (RestClientException ex) {
            throw new BadGatewayException("Keycloak create user failed: " + ex.getMessage());
        }
    }

    private boolean updateUser(String userId, KeycloakUserCommand command, String token) {
        KeycloakUserRepresentation current = getUser(userId, token);
        String[] names = splitName(command.displayName());

        boolean changed = !equalsSafe(current.email(), command.email())
            || !equalsSafe(current.firstName(), names[0])
            || !equalsSafe(current.lastName(), names[1])
            || current.enabled() != command.enabled();

        if (!changed) {
            return false;
        }

        KeycloakUserRepresentation updated = new KeycloakUserRepresentation(
            null,
            command.username(),
            command.email(),
            command.enabled(),
            names[0],
            names[1],
            Map.of("personId", List.of(command.personId()))
        );

        requestPut("/admin/realms/%s/users/%s".formatted(properties.getKeycloak().getRealm(), userId), updated, token);
        return true;
    }

    private boolean syncRoles(String userId, Set<String> desiredRoles, String token) {
        List<KeycloakRoleRepresentation> current = getUserRoles(userId, token);
        Set<String> currentNames = current.stream().map(KeycloakRoleRepresentation::name).collect(Collectors.toSet());

        Set<String> toAdd = desiredRoles.stream().filter(role -> !currentNames.contains(role)).collect(Collectors.toSet());
        Set<String> toRemove = currentNames.stream().filter(role -> !desiredRoles.contains(role)).collect(Collectors.toSet());

        if (toAdd.isEmpty() && toRemove.isEmpty()) {
            return false;
        }

        if (!toAdd.isEmpty()) {
            List<KeycloakRoleRepresentation> addReps = toAdd.stream().map(this::getRole).toList();
            requestPost("/admin/realms/%s/users/%s/role-mappings/realm".formatted(properties.getKeycloak().getRealm(), userId), addReps, token);
        }

        if (!toRemove.isEmpty()) {
            List<KeycloakRoleRepresentation> removeReps = toRemove.stream().map(this::getRole).toList();
            requestDelete("/admin/realms/%s/users/%s/role-mappings/realm".formatted(properties.getKeycloak().getRealm(), userId), removeReps, token);
        }

        return true;
    }

    private List<KeycloakRoleRepresentation> getUserRoles(String userId, String token) {
        try {
            KeycloakRoleRepresentation[] roles = keycloakRestClient.get()
                .uri("/admin/realms/%s/users/%s/role-mappings/realm".formatted(properties.getKeycloak().getRealm(), userId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(KeycloakRoleRepresentation[].class);
            return roles == null ? List.of() : Arrays.asList(roles);
        } catch (RestClientException ex) {
            throw new BadGatewayException("Keycloak get user roles failed: " + ex.getMessage());
        }
    }

    private KeycloakRoleRepresentation getRole(String roleName) {
        String token = accessToken();
        try {
            return keycloakRestClient.get()
                .uri("/admin/realms/%s/roles/%s".formatted(properties.getKeycloak().getRealm(), roleName))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(KeycloakRoleRepresentation.class);
        } catch (RestClientException ex) {
            throw new BadGatewayException("Keycloak get role failed: " + ex.getMessage());
        }
    }

    private void requestPut(String path, Object body, String token) {
        try {
            keycloakRestClient.put()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new BadGatewayException("Keycloak update failed: " + ex.getMessage());
        }
    }

    private void requestPost(String path, Object body, String token) {
        try {
            keycloakRestClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new BadGatewayException("Keycloak role mapping failed: " + ex.getMessage());
        }
    }

    private void requestDelete(String path, Object body, String token) {
        try {
            keycloakRestClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new BadGatewayException("Keycloak role unmapping failed: " + ex.getMessage());
        }
    }

    private KeycloakUserRepresentation mapUser(KeycloakUserCommand command, String id) {
        String[] names = splitName(command.displayName());
        return new KeycloakUserRepresentation(
            id,
            command.username(),
            command.email(),
            command.enabled(),
            names[0],
            names[1],
            Map.of("personId", List.of(command.personId()))
        );
    }

    private String[] splitName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return new String[]{"", ""};
        }
        String[] parts = displayName.trim().split("\\s+", 2);
        return new String[]{parts[0], parts.length > 1 ? parts[1] : ""};
    }

    private boolean equalsSafe(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private String accessToken() {
        TokenHolder cached = tokenCache.get();
        if (cached != null && cached.expiresAt().isAfter(Instant.now().plusSeconds(15))) {
            return cached.token();
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", properties.getKeycloak().getClientId());
            form.add("client_secret", properties.getKeycloak().getClientSecret());

            KeycloakTokenResponse response = keycloakRestClient.post()
                .uri("/realms/%s/protocol/openid-connect/token".formatted(properties.getKeycloak().getRealm()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KeycloakTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new BadGatewayException("Keycloak token response is empty");
            }

            TokenHolder newToken = new TokenHolder(response.accessToken(), Instant.now().plusSeconds(response.expiresIn()));
            tokenCache.set(newToken);
            return newToken.token();
        } catch (RestClientException ex) {
            throw new BadGatewayException("Keycloak token fetch failed: " + ex.getMessage());
        }
    }

    private record TokenHolder(String token, Instant expiresAt) {
    }
}
