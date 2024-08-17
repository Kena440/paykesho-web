package org.paykesho.organizationportal.client.controller;

import org.paykesho.organizationportal.client.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients/rest")
public class ClientRestController {
    @Autowired
    private ClientService clientService;

    @PostMapping("/check-client")
    public String checkClientEmail(Integer id, String email) {
        return clientService.isEmailUnique(id, email) ? "OK" : "Duplicated";
    }
}
