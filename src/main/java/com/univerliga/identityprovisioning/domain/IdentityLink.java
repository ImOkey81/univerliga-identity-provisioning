package com.univerliga.identityprovisioning.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "identity_links")
public class IdentityLink {

    @Id
    @Column(name = "person_id", nullable = false, updatable = false)
    private String personId;

    @Column(name = "keycloak_user_id")
    private String keycloakUserId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProvisioningStatus status;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "department_id")
    private String departmentId;

    @Column(name = "team_id")
    private String teamId;

    @Column(name = "last_error")
    private String lastError;

    @ElementCollection
    @CollectionTable(name = "identity_link_roles", joinColumns = @JoinColumn(name = "person_id"))
    @Column(name = "role_name")
    private Set<String> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = ProvisioningStatus.PENDING;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
