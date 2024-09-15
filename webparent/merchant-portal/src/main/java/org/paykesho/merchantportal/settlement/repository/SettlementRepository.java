package org.paykesho.merchantportal.settlement.repository;

import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Integer> {

    @Query("select s from Settlement s where s.merchant = :merchant order by s.id desc")
    List<Settlement> getSettlementsByMerchant(Merchant merchant);
}