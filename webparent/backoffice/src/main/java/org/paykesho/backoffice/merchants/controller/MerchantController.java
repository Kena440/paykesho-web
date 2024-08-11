package org.paykesho.backoffice.merchants.controller;

import org.commons.entity.State;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.settings.Setting;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.merchants.enums.Constants;
import org.paykesho.backoffice.merchants.service.CSOService;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.paging.PagingAndSortingParam;
import org.paykesho.backoffice.settings.service.SettingService;
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
public class MerchantController {

    @Autowired
    private CSOService csoService;

    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String listFirstPage() {
        return Constants.REDIRECT_TO_MERCHANTS_WITHOUT_KEYWORD;
    }

    @GetMapping("/page/{pageNum}")
    public String listAllCivilSocietyOrganizationsByPage(@PagingAndSortingParam(listName = "csoList", moduleURL = "/csos") PagingAndSortingHelper helper, @PathVariable("pageNum") int pageNum) throws PaykeshoError {
        csoService.listByPage(pageNum, helper);

        return "csos/csos";
    }

    @GetMapping("/new")
    public String newCivilSocietyOrganization(Model model) {
        String pageTitle = "New Civil Society Organization";
        Merchant cso = new Merchant();
        List<State> stateList = settingService.listStatesByCountry("Zambia");

        cso.setEnabled(true);

        model.addAttribute("cso", cso);
        model.addAttribute("stateList", stateList);
        model.addAttribute("pageTitle", pageTitle);
        return "csos/cso_form";
    }

    @PostMapping("/save")
    public String saveCivilSocietyOrganization(Merchant merchant, RedirectAttributes redirectAttributes) throws PaykeshoError {
        csoService.save(merchant);
        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Civil Society Organization Operation Successful: " + merchant.getEmail());
        return getRedirectURLtoAffectedCivilSocietyOrganization(merchant);
    }

    @GetMapping("/view/{id}")
    public String viewCivilSocietyOrganization(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) {
        if (id == null) {
            return Constants.REDIRECT_MERCHANTS;
        }
        String pageTitle = "View Civil Society Organization";

        try {

            Merchant merchant = csoService.getCivilSocietyOrganizationById(id);

            loadCurrencySetting(request);
            model.addAttribute("cso", merchant);
            model.addAttribute("pageTitle", pageTitle);

            return "csos/cso_view";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
            return Constants.REDIRECT_MERCHANTS;
        }
    }

    private String getRedirectURLtoAffectedCivilSocietyOrganization(Merchant merchant) throws PaykeshoError {
        if (merchant == null) {
            throw new PaykeshoError("no member provided");
        }
        String name = merchant.getName();

        if (merchant.getEmail() == null) {
            return Constants.REDIRECT_MERCHANTS_VIEW + merchant.getId();
        } else {
            return Constants.REDIRECT_TO_MERCHANTS_KEYWORD + name;
        }
    }

    @GetMapping("/edit/{id}")
    public String editCivilSocietyOrganization(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) {
        if (id == null) {
            return Constants.REDIRECT_MERCHANTS;
        }
        String pageTitle = "Edit CivilSocietyOrganization";

        try {
            Merchant merchant = csoService.getCivilSocietyOrganizationById(id);

            List<State> stateList = settingService.listStatesByCountry("Zambia");

            model.addAttribute("civilSocietyOrganization", merchant);
            model.addAttribute("stateList", stateList);
            model.addAttribute("pageTitle", pageTitle);

            return "csos/cso_form";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
            return Constants.REDIRECT_MERCHANTS;
        }

    }

    @GetMapping("delete/{id}")
    public String deleteCivilSocietyOrganization(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("no parameter id");
        }

        try {
            csoService.delete(id);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "CivilSocietyOrganization deleted successfully");
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
        }
        return Constants.REDIRECT_MERCHANTS;
    }

    @GetMapping("/{id}/enabled/{status}")
    public String updateCivilSocietyOrganizationEnabledStatus(@PathVariable(name = "id") Integer id, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes, Model model) throws PaykeshoError {

        if (id == null) {
            throw new PaykeshoError("no parameter id");
        }

        try {
            Merchant merchant = csoService.getCivilSocietyOrganizationById(id);
            csoService.updateCivilSocietyOrganizationStatus(id, enabled);
            String status = enabled ? "enabled" : "has been disabled";
            String operationMSG = merchant.getName() + " has been " + status;
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
        }
        return Constants.REDIRECT_MERCHANTS;

    }

    @GetMapping("/details/{id}")
    public String viewCivilSocietyOrganizationDetails(@PathVariable("id") Integer id, Model model) throws PaykeshoError {

        Merchant merchant = csoService.getCivilSocietyOrganizationById(id);
        model.addAttribute("civilSocietyOrganization", merchant);
        return "csos/civilSocietyOrganization_details_modal";

    }


    @GetMapping("/export/{format}")
    public void exportCivilSocietyOrganizationsByFormat(@PathVariable(name = "format") String format, HttpServletResponse response) throws PaykeshoError {
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
                throw new PaykeshoError("no format provided");
        }
    }


    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();

        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
