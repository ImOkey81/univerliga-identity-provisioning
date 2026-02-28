package com.univerliga.identityprovisioning.service;

import com.univerliga.identityprovisioning.domain.IdentityLink;
import com.univerliga.identityprovisioning.dto.UserStatusDto;

public final class ProvisioningMapper {

    private ProvisioningMapper() {
    }

    public static UserStatusDto toStatusDto(IdentityLink link) {
        return new UserStatusDto(
            link.getPersonId(),
            link.getKeycloakUserId(),
            link.getStatus().name(),
            link.getUsername(),
            link.getEmail(),
            link.getRoles(),
            link.isEnabled(),
            link.getLastError(),
            link.getCreatedAt(),
            link.getUpdatedAt()
        );
    }
}
