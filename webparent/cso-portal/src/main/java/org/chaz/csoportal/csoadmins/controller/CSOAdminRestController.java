package org.chaz.csoportal.csoadmins.controller;

import org.chaz.csoportal.csoadmins.service.CSOAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CSOAdminRestController {

    @Autowired
    private CSOAdminService csoAdminService;

    @PostMapping("/cso-admins/rest/check-email")
    public String checkDuplicateEmail(@RequestParam("email") String email) {
        return csoAdminService.isEmailUnique(email) ? "OK" : "Duplicated";
    }
}
