package org.paykesho.backoffice.organizationadmin.repository;


import org.commons.entity.organization.OrganizationAdmin;
import org.paykesho.backoffice.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrganizationAdminRepository extends SearchRepository<OrganizationAdmin, Integer> {

    @Query("select c from OrganizationAdmin c where concat(c.email, ' ', c.name, ' ', c.title) like %?1%")
    public Page<OrganizationAdmin> findAll(String keyword, Pageable pageable);

    @Query("update OrganizationAdmin c set c.enabled = ?2 where c.id = ?1")
    @Modifying
    public void updateEnabledStatus(Integer id, boolean enabled);

    @Query("select c from OrganizationAdmin c where c.email = ?1")
    public OrganizationAdmin findByEmail(String email);

    public Long countById(Integer id);

    Optional<OrganizationAdmin> findByUuid(String uuid);

    @Query("update OrganizationAdmin c set c.enabled = ?2 where c.uuid = ?1")
    @Modifying
    void updateEnabledStatusByUuid(String uuid, boolean enabled);

}

