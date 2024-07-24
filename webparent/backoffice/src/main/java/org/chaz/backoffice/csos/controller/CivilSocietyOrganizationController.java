package org.chaz.backoffice.csos.controller;

import org.chaz.backoffice.csos.enums.Constants;
import org.chaz.backoffice.csos.service.CSOService;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.chaz.backoffice.paging.PagingAndSortingParam;
import org.chaz.backoffice.settings.service.SettingService;
import org.commons.entity.State;
import org.commons.entity.cso.CivilSocietyOrganization;
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
@RequestMapping("/csos")
public class CivilSocietyOrganizationController {

    @Autowired
    private CSOService csoService;

    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String listFirstPage() {
        return Constants.REDIRECT_TO_CSOS_WITHOUT_KEYWORD;
    }

    @GetMapping("/page/{pageNum}")
    public String listAllCivilSocietyOrganizationsByPage(@PagingAndSortingParam(listName = "csoList", moduleURL = "/csos") PagingAndSortingHelper helper, @PathVariable("pageNum") int pageNum) throws AssessmentToolError {
        csoService.listByPage(pageNum, helper);

        return "csos/csos";
    }

    @GetMapping("/new")
    public String newCivilSocietyOrganization(Model model) {
        String pageTitle = "New Civil Society Organization";
        CivilSocietyOrganization cso = new CivilSocietyOrganization();
        List<State> stateList = settingService.listStatesByCountry("Zambia");

        cso.setEnabled(true);

        model.addAttribute("cso", cso);
        model.addAttribute("stateList", stateList);
        model.addAttribute("pageTitle", pageTitle);
        return "csos/cso_form";
    }

    @PostMapping("/save")
    public String saveCivilSocietyOrganization(CivilSocietyOrganization civilSocietyOrganization, RedirectAttributes redirectAttributes) throws AssessmentToolError {
        csoService.save(civilSocietyOrganization);
        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Civil Society Organization Operation Successful: " + civilSocietyOrganization.getEmail());
        return getRedirectURLtoAffectedCivilSocietyOrganization(civilSocietyOrganization);
    }

    @GetMapping("/view/{id}")
    public String viewCivilSocietyOrganization(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) {
        if (id == null) {
            return Constants.REDIRECT_TO_CSOS;
        }
        String pageTitle = "View Civil Society Organization";

        try {

            CivilSocietyOrganization civilSocietyOrganization = csoService.getCivilSocietyOrganizationById(id);

            loadCurrencySetting(request);
            model.addAttribute("cso", civilSocietyOrganization);
            model.addAttribute("pageTitle", pageTitle);

            return "csos/cso_view";
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, assessmentToolError.getMessage());
            return Constants.REDIRECT_TO_CSOS;
        }
    }

    private String getRedirectURLtoAffectedCivilSocietyOrganization(CivilSocietyOrganization civilSocietyOrganization) throws AssessmentToolError {
        if (civilSocietyOrganization == null) {
            throw new AssessmentToolError("no member provided");
        }
        String name = civilSocietyOrganization.getName();

        if (civilSocietyOrganization.getEmail() == null) {
            return Constants.REDIRECT_TO_CSO + civilSocietyOrganization.getId();
        } else {
            return Constants.REDIRECT_TO_CSOS_KEYWORD + name;
        }
    }

    @GetMapping("/edit/{id}")
    public String editCivilSocietyOrganization(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) {
        if (id == null) {
            return Constants.REDIRECT_TO_CSOS;
        }
        String pageTitle = "Edit CivilSocietyOrganization";

        try {
            CivilSocietyOrganization civilSocietyOrganization = csoService.getCivilSocietyOrganizationById(id);

            List<State> stateList = settingService.listStatesByCountry("Zambia");

            model.addAttribute("civilSocietyOrganization", civilSocietyOrganization);
            model.addAttribute("stateList", stateList);
            model.addAttribute("pageTitle", pageTitle);

            return "csos/cso_form";
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, assessmentToolError.getMessage());
            return Constants.REDIRECT_TO_CSOS;
        }

    }

    @GetMapping("delete/{id}")
    public String deleteCivilSocietyOrganization(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("no parameter id");
        }

        try {
            csoService.delete(id);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "CivilSocietyOrganization deleted successfully");
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, assessmentToolError.getMessage());
        }
        return Constants.REDIRECT_TO_CSOS;
    }

    @GetMapping("/{id}/enabled/{status}")
    public String updateCivilSocietyOrganizationEnabledStatus(@PathVariable(name = "id") Integer id, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes, Model model) throws AssessmentToolError {

        if (id == null) {
            throw new AssessmentToolError("no parameter id");
        }

        try {
            CivilSocietyOrganization civilSocietyOrganization = csoService.getCivilSocietyOrganizationById(id);
            csoService.updateCivilSocietyOrganizationStatus(id, enabled);
            String status = enabled ? "enabled" : "has been disabled";
            String operationMSG = civilSocietyOrganization.getName() + " has been " + status;
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, assessmentToolError.getMessage());
        }
        return Constants.REDIRECT_TO_CSOS;

    }

    @GetMapping("/details/{id}")
    public String viewCivilSocietyOrganizationDetails(@PathVariable("id") Integer id, Model model) throws AssessmentToolError {

        CivilSocietyOrganization civilSocietyOrganization = csoService.getCivilSocietyOrganizationById(id);
        model.addAttribute("civilSocietyOrganization", civilSocietyOrganization);
        return "csos/civilSocietyOrganization_details_modal";

    }


    @GetMapping("/export/{format}")
    public void exportCivilSocietyOrganizationsByFormat(@PathVariable(name = "format") String format, HttpServletResponse response) throws AssessmentToolError {
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
