package org.chaz.backoffice.memberadmin.controller;

import org.chaz.backoffice.memberadmin.service.AssessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assessors/rest")
public class AssessorRestController {
    @Autowired
    private AssessorService assessorService;

    @PostMapping("/check-email")
    public String checkCustomerEmail(@RequestParam("id") Integer id, @RequestParam("email") String email) {
        if (email == null) {
            return "redirect:/customers";
        }
        return assessorService.isEmailUnique(id, email) ? "OK" : "Duplicated";
    }
}
