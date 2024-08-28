package org.paykesho.clientportal.clients.controller;

import org.commons.entity.organization.Organization;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.organizations.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
public class ClientRestController {

    @Autowired
    private ClientService clientService;
    @Autowired
    private OrganizationService organizationService;

    @PostMapping("/rest/check-client")
    public String checkClientStatus(@RequestParam("name") String name, @RequestParam("nrc") String nrc, @RequestParam("tpin") String tpin, @RequestParam("manNumber") String manNumber, @RequestParam("organization") Integer organizationId) throws PaykeshoError {
        Organization organization = organizationService.getOrganizationById(organizationId);
        return clientService.checkClientStatus(name, nrc, tpin, manNumber, organization);
    }
}
