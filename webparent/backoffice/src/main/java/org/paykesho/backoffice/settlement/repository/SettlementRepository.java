package org.paykesho.backoffice.settlement.repository;

import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Integer> {
    List<Settlement> listAll();

    List<Settlement> getSettlementsByMerchant(Merchant merchant);

    @Query("select count(t.id) as total from Settlement t order by t.settlementDate desc")
    Long countAllSettlements();
}