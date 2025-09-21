package org.paykesho.clientportal.organizations.controller;

// import org.paykesho.clientportal.organizations.service.OrganizationService; // Unused import
// import org.springframework.beans.factory.annotation.Autowired; // Unused import

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/organizations/rest")
public class OrganizationRestController {

    // @Autowired
    // private OrganizationService organizationService; // Unused field
/*
    @GetMapping("/get-facility-types-data/{enabled}")
    public List<DataItem> getOrganizationTypesData(@PathVariable("enabled") boolean enabled) {
        return organizationService.getOrganizationTypesDataEnabled(enabled);
    }

    @GetMapping("/get-facility-gps-data/{enabled}")
    public List<GPSDataItem> getOrganizationGPSData(@PathVariable("enabled") boolean enabled) {
        return organizationService.getOrganizationGPSDataEnabled(enabled);
    }*/
}
