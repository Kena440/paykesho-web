package org.paykesho.clientportal.merchant.controller;

import org.paykesho.clientportal.merchant.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/merchants/rest")
public class MerchantRestController {
    @Autowired
    private MerchantService merchantService;


}
