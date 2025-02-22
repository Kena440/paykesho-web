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
        return merchantRepository.findAllEnabledMerchants();
    }

    public List<MerchantDto> getEnabledMerchantsFlat() {
        return merchantRepository.findAllEnabledMerchantsFlat();
    }

    public Merchant getMerchantByName(String merchant) {
        return merchantRepository.getMerchantByMerchantName(merchant);
    }

    public Merchant getMerchantByPaykeshoRef(String code) {
        return merchantRepository.getMerchantByPaykeshoRef(code);
    }

    public MerchantDto getMerchantByCode(String code) {
        return merchantRepository.getMerchantByPaykeshoId(code);
    }
}
