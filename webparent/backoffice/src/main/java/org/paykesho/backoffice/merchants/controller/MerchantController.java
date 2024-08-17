package org.paykesho.backoffice.merchants.controller;

import org.commons.entity.State;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.settings.Setting;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.merchants.enums.Constants;
import org.paykesho.backoffice.merchants.service.MerchantService;
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
@RequestMapping("/merchants")
public class MerchantController {

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String listFirstPage() {
        return Constants.REDIRECT_TO_MERCHANTS_WITHOUT_KEYWORD;
    }

    @GetMapping("/page/{pageNum}")
    public String listAllMerchantsByPage(@PagingAndSortingParam(listName = "merchantList", moduleURL = "/merchants") PagingAndSortingHelper helper, @PathVariable("pageNum") int pageNum) throws PaykeshoError {
        merchantService.listByPage(pageNum, helper);

        return "merchants/merchants";
    }

    @GetMapping("/new")
    public String newMerchant(Model model) {
        String pageTitle = "New Merchant";
        Merchant merchant = new Merchant();
        List<State> stateList = settingService.listStatesByCountry("Zambia");

        merchant.setEnabled(true);

        model.addAttribute("merchant", merchant);
        model.addAttribute("stateList", stateList);
        model.addAttribute("pageTitle", pageTitle);
        return "merchants/merchant_form";
    }

    @PostMapping("/save")
    public String saveMerchant(Merchant merchant, RedirectAttributes redirectAttributes) throws PaykeshoError {
        merchantService.save(merchant);
        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Merchant Operation Successful: " + merchant.getEmail());
        return getRedirectURLtoAffectedMerchant(merchant);
    }

    @GetMapping("/view/{id}")
    public String viewMerchant(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) {
        if (id == null) {
            return Constants.REDIRECT_MERCHANTS;
        }
        String pageTitle = "View Merchant";

        try {

            Merchant merchant = merchantService.getMerchantById(id);

            loadCurrencySetting(request);
            model.addAttribute("merchant", merchant);
            model.addAttribute("pageTitle", pageTitle);

            return "merchants/merchant_view";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
            return Constants.REDIRECT_MERCHANTS;
        }
    }

    private String getRedirectURLtoAffectedMerchant(Merchant merchant) throws PaykeshoError {
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
    public String editMerchant(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) {
        if (id == null) {
            return Constants.REDIRECT_MERCHANTS;
        }
        String pageTitle = "Edit Merchant";

        try {
            Merchant merchant = merchantService.getMerchantById(id);

            List<State> stateList = settingService.listStatesByCountry("Zambia");

            model.addAttribute("merchant", merchant);
            model.addAttribute("stateList", stateList);
            model.addAttribute("pageTitle", pageTitle);

            return "merchants/merchant_form";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
            return Constants.REDIRECT_MERCHANTS;
        }

    }

    @GetMapping("delete/{id}")
    public String deleteMerchant(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("no parameter id");
        }

        try {
            merchantService.delete(id);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Merchant deleted successfully");
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
        }
        return Constants.REDIRECT_MERCHANTS;
    }

    @GetMapping("/{id}/enabled/{status}")
    public String updateMerchantEnabledStatus(@PathVariable(name = "id") Integer id, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes, Model model) throws PaykeshoError {

        if (id == null) {
            throw new PaykeshoError("no parameter id");
        }

        try {
            Merchant merchant = merchantService.getMerchantById(id);
            merchantService.updateMerchantStatus(id, enabled);
            String status = enabled ? "enabled" : "has been disabled";
            String operationMSG = merchant.getName() + " has been " + status;
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
        }
        return Constants.REDIRECT_MERCHANTS;

    }

    @GetMapping("/details/{id}")
    public String viewMerchantDetails(@PathVariable("id") Integer id, Model model) throws PaykeshoError {

        Merchant merchant = merchantService.getMerchantById(id);
        model.addAttribute("merchant", merchant);
        return "merchants/merchant_details_modal";

    }


    @GetMapping("/export/{format}")
    public void exportMerchantsByFormat(@PathVariable(name = "format") String format, HttpServletResponse response) throws PaykeshoError {
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
