package org.paykesho.backoffice.settlement.controller;

import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.Settlement;
import org.commons.entity.settings.Setting;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.merchants.service.MerchantService;
import org.paykesho.backoffice.settings.service.SettingService;
import org.paykesho.backoffice.settlement.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("/settlements")
public class SettlementController {

    @Autowired
    private SettlementService settlementService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private MerchantService merchantService;

    @GetMapping("")
    public String showSettlements(Model model, HttpServletRequest request) {
        List<Settlement> settlementList = settlementService.listAll();
        loadCurrencySetting(request);
        model.addAttribute("settlementList", settlementList);
        model.addAttribute("pageTitle", "All Settlements");

        return "settlements/settlements";
    }

    @GetMapping("/merchant/{id}")
    public String listAllSettlementsByMerchant(@PathVariable("id") int id, Model model) throws PaykeshoError {

        Merchant merchant = merchantService.getMerchantById(id);

        List<Settlement> settlementList = settlementService.listAllByMerchant(merchant);
        model.addAttribute("settlementList", settlementList);
        model.addAttribute("pageTitle", "Merchant Settlements");

        return "settlements/settlements";
    }

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();
        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
