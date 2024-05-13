package org.chaz.backoffice.facilities.controller;

import org.chaz.membershipbackoffice.memberinstitution.dto.DataItem;
import org.chaz.membershipbackoffice.memberinstitution.dto.GPSDataItem;
import org.chaz.membershipbackoffice.memberinstitution.service.MemberInstitutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/member-institutions/rest")
public class MemberInstitutionRestController {

    @Autowired
    private MemberInstitutionService memberInstitutionService;

    @PostMapping("/check-member")
    public String checkDuplicateMember(Integer id, Integer provinceId, String memberName, String memberType) {
        return memberInstitutionService.isMemberUnique(id, provinceId, memberName, memberType) ? "OK" : "Duplicated";
    }

    @GetMapping("/get-member-types-data/{enabled}")
    public List<DataItem> getMemberTypesData(@PathVariable("enabled") boolean enabled) {
        return memberInstitutionService.getMemberTypesDataEnabled(enabled);
    }

    @GetMapping("/get-member-gps-data/{enabled}")
    public List<GPSDataItem> getMemberGPSData(@PathVariable("enabled") boolean enabled) {
        return memberInstitutionService.getMemberGPSDataEnabled(enabled);
    }
}
