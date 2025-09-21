package org.paykesho.clientportal.clients.controller;

import org.commons.entity.organization.Organization;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.organizations.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
public class ClientRestController {

    private static final Logger log = LoggerFactory.getLogger(ClientRestController.class);

    @Autowired
    private ClientService clientService;
    @Autowired
    private OrganizationService organizationService;

    @PostMapping("/rest/check-client")
    public String checkClientStatus(@RequestParam("name") String name, @RequestParam("nrc") String nrc, @RequestParam("tpin") String tpin, @RequestParam("manNumber") String manNumber, @RequestParam("organization") Integer organizationId) {
        try {
            log.debug("Checking client status for name: {}, nrc: {}, organization: {}", name, nrc, organizationId);

            Organization organization = null;
            try {
                organization = organizationService.getOrganizationById(organizationId);
                if (organization == null) {
                    log.warn("Organization not found for ID: {}", organizationId);
                    return "Organization not found";
                }
            } catch (Exception ex) {
                log.error("Error retrieving organization: {}", ex.getMessage(), ex);
                return "Failed to retrieve organization";
            }

            String result = clientService.checkClientStatus(name, nrc, tpin, manNumber, organization);
            log.debug("Client status check result: {}", result);
            return result;

        } catch (Exception ex) {
            log.error("Error checking client status: {}", ex.getMessage(), ex);
            return "Failed to check client status";
        }
    }
}
