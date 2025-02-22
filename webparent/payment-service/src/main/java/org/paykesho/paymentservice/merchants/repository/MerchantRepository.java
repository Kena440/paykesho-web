package org.paykesho.paymentservice.merchants.repository;

import org.commons.entity.merchant.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MerchantRepository extends JpaRepository<Merchant, Integer> {
    @Query("select m " + "from Merchant m " + "where m.enabled = ?1 ")
    List<Merchant> findMerchantTypesEnabled(boolean enabled);

}
