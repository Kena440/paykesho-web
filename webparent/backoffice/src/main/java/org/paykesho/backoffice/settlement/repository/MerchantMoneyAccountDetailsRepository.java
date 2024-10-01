package org.paykesho.backoffice.settlement.repository;

import org.commons.entity.merchant.MerchantMoneyAccountDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantMoneyAccountDetailsRepository extends JpaRepository<MerchantMoneyAccountDetail, Integer> {
    MerchantMoneyAccountDetail getMerchantMoneyAccountDetailByMerchantId(Integer id);
}