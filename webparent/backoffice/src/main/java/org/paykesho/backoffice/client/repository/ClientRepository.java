package org.paykesho.backoffice.client.repository;

import org.commons.entity.client.Client;
import org.commons.entity.organization.Organization;
import org.paykesho.backoffice.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClientRepository extends SearchRepository<Client, Integer> {

    @Query("select c from Client c where concat(c.code, ' ', c.email, ' ', c.name, ' ', c.address, ' ', c.phoneNumber, ' ', c.clientType, ' ', c.salutation) like %?1%")
    public Page<Client> findAll(String keyword, Pageable pageable);

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
}

