package org.chaz.backoffice.csos.controller;

import org.chaz.backoffice.csos.service.CSOService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/csos/rest")
public class CivilSocietyOrganizationRestController {

    @Autowired
    private CSOService CSOService;

    @PostMapping("/check-cso")
    public String checkDuplicateCivilSocietyOrganization(Integer id, Integer provinceId, String csoName, String district) {
        return CSOService.isCivilSocietyOrganizationUnique(id, provinceId, csoName, district) ? "OK" : "Duplicated";
    }
}
