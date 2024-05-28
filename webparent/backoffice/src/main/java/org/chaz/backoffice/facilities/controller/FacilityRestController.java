package org.chaz.backoffice.facilities.controller;

import org.chaz.backoffice.facilities.dto.DataItem;
import org.chaz.backoffice.facilities.dto.GPSDataItem;
import org.chaz.backoffice.facilities.service.FacilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/facilities/rest")
public class FacilityRestController {

    @Autowired
    private FacilityService facilityService;

    @PostMapping("/check-facility")
    public String checkDuplicateFacility(Integer id, Integer provinceId, String facilityName, Integer levelOfCare) {
        return facilityService.isFacilityUnique(id, provinceId, facilityName, levelOfCare) ? "OK" : "Duplicated";
    }

    @GetMapping("/get-facility-types-data/{enabled}")
    public List<DataItem> getFacilityTypesData(@PathVariable("enabled") boolean enabled) {
        return facilityService.getFacilityTypesDataEnabled(enabled);
    }

    @GetMapping("/get-facility-gps-data/{enabled}")
    public List<GPSDataItem> getFacilityGPSData(@PathVariable("enabled") boolean enabled) {
        return facilityService.getFacilityGPSDataEnabled(enabled);
    }
}
