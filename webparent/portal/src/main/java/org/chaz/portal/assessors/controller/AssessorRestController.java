package org.chaz.portal.assessors.controller;

import org.chaz.portal.assessors.service.AssessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AssessorRestController {

    @Autowired
    private AssessorService assessorService;

    @PostMapping("/assessors/rest/check-email")
    public String checkDuplicateEmail(@RequestParam("email") String email) {
        return assessorService.isEmailUnique(email) ? "OK" : "Duplicated";
    }
}
