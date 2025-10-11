package org.paykesho.backoffice.merchants.controller;

import org.paykesho.backoffice.merchants.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/merchants/rest")
public class MerchantRestController {

    @Autowired
    private MerchantService merchantService;

    @PostMapping("/check-merchant")
    public String checkDuplicateMerchant(String uuid, String merchantName, String merchantType) {
        return merchantService.isMerchantUniqueByUuid(uuid, merchantName, merchantType) ? "OK" : "Duplicated";
    }
}
