package org.paykesho.merchantportal.merchantadmins.controller;

import org.paykesho.merchantportal.merchantadmins.service.MerchantAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MerchantAdminRestController {

    @Autowired
    private MerchantAdminService merchantAdminService;

    @PostMapping("/cso-admins/rest/check-email")
    public String checkDuplicateEmail(@RequestParam("email") String email) {
        return merchantAdminService.isEmailUnique(email) ? "OK" : "Duplicated";
    }
}
