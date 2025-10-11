package org.paykesho.organizationportal.client.repository;

import org.commons.entity.client.Client;
import org.commons.entity.organization.Organization;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends CrudRepository<Client, Integer> {

    @Query("update Client c set c.enabled = ?2 where c.id = ?1")
    @Modifying
    public void updateEnabledStatus(Integer id, boolean enabled);

    @Query("select c from Client c where c.email = ?1")
    public Client findByEmail(String email);

    public Long countById(Integer id);

    @Query("select count(c.id) from Client c")
    public Long countAllClients();

    @Query("select c from Client c where c.organization = ?1")
    List<Client> getOrganizationClients(Organization organization);

    // UUID-based methods
    Optional<Client> findByUuid(String uuid);
    
    @Query("SELECT COUNT(c) > 0 FROM Client c WHERE c.uuid = ?1")
    boolean existsByUuid(String uuid);
}

