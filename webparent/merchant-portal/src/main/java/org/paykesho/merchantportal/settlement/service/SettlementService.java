package org.paykesho.merchantportal.settlement.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.Settlement;
import org.paykesho.merchantportal.settlement.repository.SettlementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SettlementService {

    @Autowired
    private SettlementRepository settlementRepository;

    public List<Settlement> listAllByMerchant(Merchant merchant) {
        return settlementRepository.getSettlementsByMerchant(merchant);
    }

}
