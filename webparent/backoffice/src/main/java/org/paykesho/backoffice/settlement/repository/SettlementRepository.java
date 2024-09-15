package org.paykesho.backoffice.settlement.repository;

import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Integer> {

    @Query("select s from Settlement s order by s.id desc")
    List<Settlement> getAllSettlements();

    @Query("select s from Settlement s where s.merchant = :merchant order by s.id desc")
    List<Settlement> getSettlementsByMerchant(Merchant merchant);

    @Query("select count(t.id) as total from Settlement t order by t.settlementDate desc")
    Long countAllSettlements();
}