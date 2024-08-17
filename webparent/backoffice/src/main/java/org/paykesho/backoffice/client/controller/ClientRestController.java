package org.paykesho.backoffice.client.controller;

import org.paykesho.backoffice.client.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients/rest")
public class ClientRestController {
    @Autowired
    private ClientService customerService;

    @PostMapping("/check-email")
    public String checkCustomerEmail(Integer id, String email) {
        return customerService.isEmailUnique(id, email) ? "OK" : "Duplicated";
    }
}
