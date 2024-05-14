package org.chaz.backoffice.facilities.controller;

import org.chaz.backoffice.facilities.enums.Constants;
import org.chaz.backoffice.facilities.service.FacilityService;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.chaz.backoffice.paging.PagingAndSortingParam;
import org.chaz.backoffice.settings.service.SettingService;
import org.commons.entity.State;
import org.commons.entity.facilities.Facility;
import org.commons.entity.facilities.LevelOfCare;
import org.commons.entity.settings.Setting;
import org.commons.utils.AssessmentToolError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping("/facilities")
public class FacilityController {

    @Autowired
    private FacilityService facilityService;

    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String listFirstPage() {
        return Constants.REDIRECT_TO_MEMBERS_WITHOUT_KEYWORD;
    }

    @GetMapping("/page/{pageNum}")
    public String listAllFacilitysByPage(@PagingAndSortingParam(listName = "memberList", moduleURL = "/facilities") PagingAndSortingHelper helper, @PathVariable("pageNum") int pageNum) throws AssessmentToolError {
        facilityService.listByPage(pageNum, helper);

        return "facilities/facilities";
    }

    @GetMapping("/new")
    public String newFacility(Model model) {
        String pageTitle = "New Facility";
        Facility facility = new Facility();
        List<LevelOfCare> levelOfCareList = settingService.levelOfCareList();
        List<State> stateList = settingService.listStatesByCountry("Zambia");

        facility.setEnabled(true);

        model.addAttribute("facility", facility);
        model.addAttribute("levelOfCareList", levelOfCareList);
        model.addAttribute("stateList", stateList);
        model.addAttribute("pageTitle", pageTitle);
        return "facilities/facility_form";
    }

    @PostMapping("/save")
    public String saveFacility(Facility facility, RedirectAttributes redirectAttributes) throws AssessmentToolError {
        facilityService.save(facility);

        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Facility Operation Successful: " + facility.getEmail());
        return getRedirectURLtoAffectedFacility(facility);
    }

    @GetMapping("/view/{id}")
    public String viewFacility(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) {
        if (id == null) {
            return Constants.REDIRECT_TO_MEMBERS;
        }
        String pageTitle = "View Facility";

        try {

            Facility facility = facilityService.getFacilityById(id);

            loadCurrencySetting(request);
            model.addAttribute("facility", facility);
            model.addAttribute("pageTitle", pageTitle);

            return "facilities/facility_view";
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, assessmentToolError.getMessage());
            return Constants.REDIRECT_TO_MEMBERS;
        }
    }

    private String getRedirectURLtoAffectedFacility(Facility facility) throws AssessmentToolError {
        if (facility == null) {
            throw new AssessmentToolError("no member provided");
        }
        String firstPartOfEmail = facility.getEmail().split("@")[0];

        if (facility.getEmail() == null) {
            return Constants.REDIRECT_TO_MEMBER + facility.getId();
        } else {
            return Constants.REDIRECT_TO_MEMBERS_KEYWORD + firstPartOfEmail;
        }
    }

    @GetMapping("/edit/{id}")
    public String editFacility(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) {
        if (id == null) {
            return Constants.REDIRECT_TO_MEMBERS;
        }
        String pageTitle = "Edit Facility";

        try {
            Facility facility = facilityService.getFacilityById(id);

            List<LevelOfCare> levelOfCareList = settingService.levelOfCareList();
            List<State> stateList = settingService.listStatesByCountry("Zambia");

            model.addAttribute("facility", facility);
            model.addAttribute("levelOfCareList", levelOfCareList);
            model.addAttribute("stateList", stateList);
            model.addAttribute("pageTitle", pageTitle);

            return "facilities/facility_form";
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, assessmentToolError.getMessage());
            return Constants.REDIRECT_TO_MEMBERS;
        }

    }

    @GetMapping("delete/{id}")
    public String deleteFacility(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("no parameter id");
        }

        try {
            facilityService.delete(id);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Facility deleted successfully");
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, assessmentToolError.getMessage());
        }
        return Constants.REDIRECT_TO_MEMBERS;
    }

    @GetMapping("/{id}/enabled/{status}")
    public String updateFacilityEnabledStatus(@PathVariable(name = "id") Integer id, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes, Model model) throws AssessmentToolError {

        if (id == null) {
            throw new AssessmentToolError("no parameter id");
        }

        try {
            Facility facility = facilityService.getFacilityById(id);
            facilityService.updateFacilityStatus(id, enabled);
            String status = enabled ? "enabled" : "has been disabled";
            String operationMSG = facility.getName() + " has been " + status;
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, assessmentToolError.getMessage());
        }
        return Constants.REDIRECT_TO_MEMBERS;

    }

    @GetMapping("/details/{id}")
    public String viewFacilityDetails(@PathVariable("id") Integer id, Model model) throws AssessmentToolError {

        Facility facility = facilityService.getFacilityById(id);
        model.addAttribute("facility", facility);
        return "facilities/facility_details_modal";

    }


    @GetMapping("/export/{format}")
    public void exportFacilitysByFormat(@PathVariable(name = "format") String format, HttpServletResponse response) throws AssessmentToolError {
        switch (format) {
            case "csv":
                //(response);
                break;
            case "excel":
                //exportToExcel(response);
                break;
            case "pdf":
                //exportToPDF(response);
                break;
            default:
                throw new AssessmentToolError("no format provided");
        }
    }


    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();

        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
