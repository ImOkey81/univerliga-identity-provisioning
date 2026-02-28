package com.univerliga.identityprovisioning.service;

import com.univerliga.identityprovisioning.domain.IdentityLink;
import com.univerliga.identityprovisioning.domain.ProvisioningStatus;
import com.univerliga.identityprovisioning.dto.PageInfo;
import com.univerliga.identityprovisioning.dto.PagedUsersResponse;
import com.univerliga.identityprovisioning.dto.ReconcileResponse;
import com.univerliga.identityprovisioning.dto.UserStatusDto;
import com.univerliga.identityprovisioning.dto.UserToggleResponse;
import com.univerliga.identityprovisioning.dto.UserUpdateRequest;
import com.univerliga.identityprovisioning.dto.UserUpsertRequest;
import com.univerliga.identityprovisioning.exception.NotFoundException;
import com.univerliga.identityprovisioning.keycloak.KeycloakAdminClient;
import com.univerliga.identityprovisioning.keycloak.KeycloakProvisionedUser;
import com.univerliga.identityprovisioning.keycloak.KeycloakUserCommand;
import com.univerliga.identityprovisioning.repository.IdentityLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProvisioningService {

    private final IdentityLinkRepository identityLinkRepository;
    private final KeycloakAdminClient keycloakAdminClient;

    @Transactional
    public UserStatusDto createOrLink(UserUpsertRequest request) {
        Optional<IdentityLink> existing = identityLinkRepository.findById(request.personId());
        IdentityLink link = existing.orElseGet(IdentityLink::new);
        link.setPersonId(request.personId());
        applyUpsert(link, request.username(), request.email(), request.displayName(), request.departmentId(), request.teamId(), request.roles(), request.enabled());

        KeycloakProvisionedUser provisioned = keycloakAdminClient.createOrUpdate(toKeycloakCommand(link));
        link.setKeycloakUserId(provisioned.userId());
        link.setStatus(link.isEnabled() ? ProvisioningStatus.PROVISIONED : ProvisioningStatus.DEPROVISIONED);
        link.setLastError(null);

        return ProvisioningMapper.toStatusDto(identityLinkRepository.save(link));
    }

    @Transactional
    public UserStatusDto update(String personId, UserUpdateRequest request) {
        IdentityLink link = identityLinkRepository.findById(personId)
            .orElseThrow(() -> new NotFoundException("Identity link not found for personId=" + personId));

        applyUpsert(link, request.username() == null ? link.getUsername() : request.username(), request.email(), request.displayName(),
            request.departmentId(), request.teamId(), request.roles(), request.enabled());

        KeycloakProvisionedUser provisioned = keycloakAdminClient.createOrUpdate(toKeycloakCommand(link));
        link.setKeycloakUserId(provisioned.userId());
        link.setStatus(link.isEnabled() ? ProvisioningStatus.PROVISIONED : ProvisioningStatus.DEPROVISIONED);
        link.setLastError(null);

        return ProvisioningMapper.toStatusDto(identityLinkRepository.save(link));
    }

    @Transactional
    public UserToggleResponse toggle(String personId, boolean enabled) {
        IdentityLink link = identityLinkRepository.findById(personId)
            .orElseThrow(() -> new NotFoundException("Identity link not found for personId=" + personId));

        keycloakAdminClient.setEnabled(link.getKeycloakUserId(), enabled);
        link.setEnabled(enabled);
        link.setStatus(enabled ? ProvisioningStatus.PROVISIONED : ProvisioningStatus.DEPROVISIONED);
        identityLinkRepository.save(link);

        return new UserToggleResponse(link.getPersonId(), enabled, link.getStatus().name(), link.getUpdatedAt());
    }

    @Transactional
    public ReconcileResponse reconcile(String personId) {
        IdentityLink link = identityLinkRepository.findById(personId)
            .orElseThrow(() -> new NotFoundException("Identity link not found for personId=" + personId));

        KeycloakProvisionedUser result = keycloakAdminClient.reconcile(toKeycloakCommand(link), link.getKeycloakUserId());
        link.setStatus(link.isEnabled() ? ProvisioningStatus.PROVISIONED : ProvisioningStatus.DEPROVISIONED);
        identityLinkRepository.save(link);

        return new ReconcileResponse(personId, result.changed() ? "CHANGED" : "OK", OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public UserStatusDto get(String personId) {
        IdentityLink link = identityLinkRepository.findById(personId)
            .orElseThrow(() -> new NotFoundException("Identity link not found for personId=" + personId));
        return ProvisioningMapper.toStatusDto(link);
    }

    @Transactional(readOnly = true)
    public PagedUsersResponse list(String query, ProvisioningStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<IdentityLink> result = identityLinkRepository.search(status, query, pageable);

        return new PagedUsersResponse(
            result.getContent().stream().map(ProvisioningMapper::toStatusDto).toList(),
            new PageInfo(page, size, result.getTotalElements(), result.getTotalPages())
        );
    }

    @Transactional
    public UserStatusDto upsertFromEvent(String personId, String username, String email, String displayName,
                                         java.util.Set<String> roles, Boolean enabled) {
        UserUpsertRequest request = new UserUpsertRequest(personId, username, email, displayName, null, null, roles, enabled);
        return createOrLink(request);
    }

    @Transactional
    public void markFailed(String personId, String error) {
        identityLinkRepository.findById(personId).ifPresent(link -> {
            link.setStatus(ProvisioningStatus.FAILED);
            link.setLastError(error);
            identityLinkRepository.save(link);
        });
    }

    private void applyUpsert(IdentityLink link, String username, String email, String displayName, String departmentId,
                             String teamId, java.util.Set<String> roles, Boolean enabled) {
        link.setUsername(username);
        link.setEmail(email);
        link.setDisplayName(displayName);
        link.setDepartmentId(departmentId);
        link.setTeamId(teamId);
        link.setRoles(new HashSet<>(roles));
        link.setEnabled(enabled == null || enabled);
    }

    private KeycloakUserCommand toKeycloakCommand(IdentityLink link) {
        return new KeycloakUserCommand(
            link.getPersonId(),
            link.getUsername(),
            link.getEmail(),
            link.getDisplayName(),
            link.getRoles(),
            link.isEnabled()
        );
    }
}
