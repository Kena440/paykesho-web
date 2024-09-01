package org.paykesho.clientportal.merchant.repository;

import org.commons.entity.merchant.Merchant;
import org.paykesho.clientportal.api.dto.MerchantDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MerchantRepository extends JpaRepository<Merchant, Integer> {
    @Query("select m from Merchant m where m.enabled = true order by m.name desc")
    public List<Merchant> findAllEnabledMerchants();

    @Query("select s from Merchant s where s.name = :name")
    public Merchant getMerchantByMerchantName(@Param("name") String name);

    @Query("select count(m.id) as total from Merchant m")
    public Long countAllMerchants();

    @Query("select new org.paykesho.clientportal.api.dto.MerchantDto(m.id, m.code, m.name, m.details, m.photos) from Merchant m where m.enabled = true order by m.name desc")
    List<MerchantDto> findAllEnabledMerchantsFlat();

    @Query("select new org.paykesho.clientportal.api.dto.MerchantDto(m.id, m.code, m.name, m.details, m.photos) from Merchant m where m.code = :code")
    MerchantDto getMerchantByPaykeshoId(@Param("code") String code);

    @Query("select m from Merchant m where m.code = :code")
    Merchant getMerchantByPaykeshoRef(@Param("code") String code);
}
