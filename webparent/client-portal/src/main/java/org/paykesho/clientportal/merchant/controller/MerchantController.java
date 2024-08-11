package org.paykesho.clientportal.merchant.controller;

import org.paykesho.clientportal.merchant.service.MerchantService;
import org.paykesho.clientportal.settings.service.SettingService;
import org.paykesho.clientportal.utils.PaykeshoReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/merchants")
public class MerchantController {
    @Autowired
    private PaykeshoReferenceService paykeshoReferenceService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private MerchantService merchantService;

}
