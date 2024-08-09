package org.paykesho.clientportal.clients.controller;

import org.paykesho.clientportal.clients.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClientRestController {

    @Autowired
    private ClientService clientService;

    @PostMapping("/cso-admins/rest/check-email")
    public String checkDuplicateEmail(@RequestParam("email") String email) {
        return clientService.isEmailUnique(email) ? "OK" : "Duplicated";
    }
}
