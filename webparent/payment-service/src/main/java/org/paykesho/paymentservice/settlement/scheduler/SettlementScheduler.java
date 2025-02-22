package org.paykesho.paymentservice.settlement.scheduler;

import org.commons.entity.merchant.Merchant;
import org.commons.utils.PaykeshoError;
import org.paykesho.paymentservice.merchants.repository.MerchantRepository;
import org.paykesho.paymentservice.settlement.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SettlementScheduler {
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private SettlementService settlementService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void settleAllMerchants() throws PaykeshoError {
        List<Merchant> merchants = merchantRepository.findMerchantTypesEnabled(true);

        for (Merchant merchant : merchants) {
            settlementService.reconcileAndSettleForMerchant(merchant);
        }
    }
}
