package org.paykesho.clientportal.merchant.controller;

import org.paykesho.clientportal.api.dto.MerchantDto;
import org.paykesho.clientportal.merchant.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/merchants/rest")
public class MerchantRestController {

    @Autowired
    private MerchantService merchantService;

    @GetMapping("/merchant-details")
    public ResponseEntity<MerchantDto> getMerchantDetails(@RequestParam("code") String merchantCode) {
        try {
            MerchantDto merchant = merchantService.getMerchantByCode(merchantCode);
            if (merchant != null) {
                return ResponseEntity.ok(merchant);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}

