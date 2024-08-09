package org.paykesho.merchantportal.merchants.controller;

import org.paykesho.merchantportal.merchants.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/merchants/rest")
public class MerchantRestController {

    @Autowired
    private MerchantService facilityService;

/*    @GetMapping("/get-facility-types-data/{enabled}")
    public List<DataItem> getOrganizationTypesData(@PathVariable("enabled") boolean enabled) {
        return facilityService.getOrganizationTypesDataEnabled(enabled);
    }

    @GetMapping("/get-facility-gps-data/{enabled}")
    public List<GPSDataItem> getOrganizationGPSData(@PathVariable("enabled") boolean enabled) {
        return facilityService.getOrganizationGPSDataEnabled(enabled);
    }*/
}
