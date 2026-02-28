package com.univerliga.identityprovisioning.service;

import com.univerliga.identityprovisioning.domain.IdentityLink;
import com.univerliga.identityprovisioning.domain.ProvisioningStatus;
import com.univerliga.identityprovisioning.dto.UserUpsertRequest;
import com.univerliga.identityprovisioning.keycloak.KeycloakAdminClient;
import com.univerliga.identityprovisioning.keycloak.KeycloakProvisionedUser;
import com.univerliga.identityprovisioning.repository.IdentityLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProvisioningServiceTest {

    @Mock
    private IdentityLinkRepository identityLinkRepository;
    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    private ProvisioningService provisioningService;

    @BeforeEach
    void setUp() {
        provisioningService = new ProvisioningService(identityLinkRepository, keycloakAdminClient);
    }

    @Test
    void createOrLink_happyPath() {
        UserUpsertRequest request = new UserUpsertRequest(
            "p_1", "user1", "user1@example.com", "User One", "d1", "t1", Set.of("ROLE_EMPLOYEE"), true
        );

        when(identityLinkRepository.findById("p_1")).thenReturn(Optional.empty());
        when(keycloakAdminClient.createOrUpdate(any())).thenReturn(new KeycloakProvisionedUser("kc-1", true));
        when(identityLinkRepository.save(any(IdentityLink.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = provisioningService.createOrLink(request);

        assertThat(response.personId()).isEqualTo("p_1");
        assertThat(response.status()).isEqualTo(ProvisioningStatus.PROVISIONED.name());
        assertThat(response.keycloakUserId()).isEqualTo("kc-1");

        ArgumentCaptor<IdentityLink> captor = ArgumentCaptor.forClass(IdentityLink.class);
        verify(identityLinkRepository).save(captor.capture());
        assertThat(captor.getValue().getRoles()).containsExactly("ROLE_EMPLOYEE");
    }
}
