package org.paykesho.merchantportal.settlement.repository;

import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Integer> {
    List<Settlement> getSettlementsByMerchant(Merchant merchant);
}