package org.paykesho.organizationportal.organizationadmins.controller;

import org.paykesho.organizationportal.organizationadmins.service.OrganizationAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrganizationAdminRestController {

    @Autowired
    private OrganizationAdminService organizationAdminService;

    @PostMapping("/cso-admins/rest/check-email")
    public String checkDuplicateEmail(@RequestParam("email") String email) {
        return organizationAdminService.isEmailUnique(email) ? "OK" : "Duplicated";
    }
}
