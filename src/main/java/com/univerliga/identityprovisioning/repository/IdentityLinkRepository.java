package com.univerliga.identityprovisioning.repository;

import com.univerliga.identityprovisioning.domain.IdentityLink;
import com.univerliga.identityprovisioning.domain.ProvisioningStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdentityLinkRepository extends JpaRepository<IdentityLink, String> {

    Page<IdentityLink> findByStatus(ProvisioningStatus status, Pageable pageable);

    @Query("""
        select i from IdentityLink i
        where (:status is null or i.status = :status)
          and (:query is null or :query = '' or
               lower(i.personId) like lower(concat('%', :query, '%')) or
               lower(i.username) like lower(concat('%', :query, '%')) or
               lower(i.email) like lower(concat('%', :query, '%')))
        """)
    Page<IdentityLink> search(@Param("status") ProvisioningStatus status, @Param("query") String query, Pageable pageable);
}
