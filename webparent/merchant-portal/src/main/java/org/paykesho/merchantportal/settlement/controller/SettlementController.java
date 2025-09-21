package org.paykesho.merchantportal.settlement.controller;

import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.MerchantAdmin;
import org.commons.entity.merchant.Settlement;
import org.commons.entity.settings.Setting;
import org.commons.utils.PaykeshoError;
import org.paykesho.merchantportal.merchantadmins.service.MerchantAdminService;
import org.paykesho.merchantportal.merchants.service.MerchantService;
import org.paykesho.merchantportal.security.MerchantAdminUserDetails;
import org.paykesho.merchantportal.settings.service.SettingService;
import org.paykesho.merchantportal.settlement.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
    @Autowired
    private MerchantAdminService merchantAdminService;

    @GetMapping("")
    public String showSettlements(Model model, HttpServletRequest request, @AuthenticationPrincipal MerchantAdminUserDetails loggedInUser) throws PaykeshoError {
        MerchantAdmin merchantAdmin = merchantAdminService.getMerchantAdminByEmail(loggedInUser.getMerchantAdmin().getEmail());
        Merchant merchant = merchantService.getMerchantByContactPerson(merchantAdmin);

        if (merchant == null) {
            // Handle case where merchant is not found
            model.addAttribute("settlementList", List.of());
            model.addAttribute("pageTitle", "All Settlements");
            loadCurrencySetting(request);
            return "settlements/settlements";
        }

        List<Settlement> settlementList = settlementService.listAllByMerchant(merchant);
        loadCurrencySetting(request);
        model.addAttribute("settlementList", settlementList);
        model.addAttribute("pageTitle", "All Settlements");

        return "settlements/settlements";
    }

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();
        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
