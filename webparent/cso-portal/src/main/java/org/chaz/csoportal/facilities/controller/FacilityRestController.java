package org.chaz.csoportal.facilities.controller;

import org.chaz.csoportal.facilities.dto.DataItem;
import org.chaz.csoportal.facilities.dto.GPSDataItem;
import org.chaz.csoportal.facilities.service.FacilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/facilities/rest")
public class FacilityRestController {

    @Autowired
    private FacilityService facilityService;

    @GetMapping("/get-facility-types-data/{enabled}")
    public List<DataItem> getFacilityTypesData(@PathVariable("enabled") boolean enabled) {
        return facilityService.getFacilityTypesDataEnabled(enabled);
    }

    @GetMapping("/get-facility-gps-data/{enabled}")
    public List<GPSDataItem> getFacilityGPSData(@PathVariable("enabled") boolean enabled) {
        return facilityService.getFacilityGPSDataEnabled(enabled);
    }
}
