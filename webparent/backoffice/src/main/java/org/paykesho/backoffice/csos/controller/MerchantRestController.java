package org.paykesho.backoffice.csos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/csos/rest")
public class MerchantRestController {

    @Autowired
    private org.paykesho.backoffice.csos.service.CSOService CSOService;

    @PostMapping("/check-cso")
    public String checkDuplicateCivilSocietyOrganization(Integer id, Integer provinceId, String csoName, String district) {
        return CSOService.isCivilSocietyOrganizationUnique(id, provinceId, csoName, district) ? "OK" : "Duplicated";
    }
}
