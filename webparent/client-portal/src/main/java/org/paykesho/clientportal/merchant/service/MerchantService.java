package org.paykesho.clientportal.merchant.service;

import lombok.extern.slf4j.Slf4j;
import org.commons.entity.merchant.Merchant;
import org.paykesho.clientportal.api.dto.MerchantDto;
import org.paykesho.clientportal.merchant.repository.MerchantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MerchantService {
    @Autowired
    private MerchantRepository merchantRepository;

    public List<Merchant> listAllEnabledMerchants() {
        try {
            log.debug("Retrieving all enabled merchants");
            List<Merchant> merchants = merchantRepository.findAllEnabledMerchants();
            log.debug("Successfully retrieved {} enabled merchants", merchants.size());
            return merchants;
        } catch (Exception ex) {
            log.error("Error retrieving enabled merchants: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve enabled merchants", ex);
        }
    }

    public List<MerchantDto> getEnabledMerchantsFlat() {
        return merchantRepository.findAllEnabledMerchantsFlat();
    }

    public Merchant getMerchantByName(String merchant) {
        try {
            log.debug("Retrieving merchant by name: {}", merchant);
            Merchant result = merchantRepository.getMerchantByMerchantName(merchant);
            if (result != null) {
                log.debug("Found merchant with ID: {}", result.getId());
            } else {
                log.debug("No merchant found with name: {}", merchant);
            }
            return result;
        } catch (Exception ex) {
            log.error("Error retrieving merchant by name {}: {}", merchant, ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve merchant by name", ex);
        }
    }

    public Merchant getMerchantByPaykeshoRef(String code) {
        return merchantRepository.getMerchantByPaykeshoRef(code);
    }

    public MerchantDto getMerchantByCode(String code) {
        return merchantRepository.getMerchantByPaykeshoId(code);
    }
}
