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
    public String checkClientStatus(@RequestParam("name") String name, 
                                    @RequestParam("nrc") String nrc, 
                                    @RequestParam("tpin") String tpin, 
                                    @RequestParam("manNumber") String manNumber, 
                                    @RequestParam("organizationUuid") String organizationUuid,
                                    @RequestParam(value = "altName", required = false) String altName) {
        try {
            log.debug("Checking employee eligibility for organization UUID: {}", organizationUuid);

            // Validate required parameters
            if (name == null || name.trim().isEmpty()) {
                log.warn("Name parameter is missing or empty");
                return "Not Found"; // Treat as validation failure
            }
            if (nrc == null || nrc.trim().isEmpty()) {
                log.warn("NRC parameter is missing or empty");
                return "Not Found";
            }
            if (organizationUuid == null || organizationUuid.trim().isEmpty()) {
                log.warn("Organization UUID parameter is missing or empty");
                return "Organization not found";
            }

            Organization organization = null;
            try {
                organization = organizationService.getOrganizationByUuid(organizationUuid);
                if (organization == null) {
                    log.warn("Organization not found for UUID: {}", organizationUuid);
                    return "Organization not found";
                }
            } catch (Exception ex) {
                log.error("Error retrieving employer record: {}", ex.getMessage());
                return "Failed to retrieve organization";
            }

            // altName: the name without the middle name, in case the staff list omits it.
            java.util.List<String> names = new java.util.ArrayList<>();
            names.add(name.trim());
            if (altName != null && !altName.isBlank()) names.add(altName.trim());
            String result = clientService.checkClientStatus(names, nrc, tpin, manNumber, organization);
            log.debug("Client status check result: {}", result);
            return result;

        } catch (Exception ex) {
            log.error("Employee eligibility check failed: {}", ex.getMessage());
            return "Failed to check client status";
        }
    }
}
