package org.paykesho.backoffice.organizations.controller;

import org.paykesho.backoffice.organizations.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/organizations/rest")
public class OrganizationRestController {

    @Autowired
    private OrganizationService organizationService;

    @PostMapping("/check-organization")
    public String checkDuplicateOrganization(Integer id, Integer provinceId, String facilityName, Integer organizationType) {
        return organizationService.isOrganizationUnique(id, provinceId, facilityName, organizationType) ? "OK" : "Duplicated";
    }

}
