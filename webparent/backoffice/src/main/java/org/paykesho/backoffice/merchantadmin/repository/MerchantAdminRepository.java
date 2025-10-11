package org.paykesho.backoffice.merchantadmin.repository;


import org.commons.entity.merchant.MerchantAdmin;
import org.paykesho.backoffice.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MerchantAdminRepository extends SearchRepository<MerchantAdmin, Integer> {

    @Query("select c from MerchantAdmin c where concat(c.email, ' ', c.name, ' ', c.title) like %?1%")
    public Page<MerchantAdmin> findAll(String keyword, Pageable pageable);

    @Query("update MerchantAdmin c set c.enabled = ?2 where c.id = ?1")
    @Modifying
    public void updateEnabledStatus(Integer id, boolean enabled);

    @Query("select c from MerchantAdmin c where c.email = ?1")
    public MerchantAdmin findByEmail(String email);

    public Long countById(Integer id);

    Optional<MerchantAdmin> findByUuid(String uuid);

}

