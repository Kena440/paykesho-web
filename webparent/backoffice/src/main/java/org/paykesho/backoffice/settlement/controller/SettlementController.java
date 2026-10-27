package org.paykesho.backoffice.settlement.controller;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.Settlement;
import org.commons.entity.settings.Setting;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.merchants.service.MerchantService;
import org.paykesho.backoffice.settings.service.SettingService;
import org.paykesho.backoffice.settlement.service.SettlementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/settlements")
public class SettlementController {


    private final SettlementService settlementService;
    private final SettingService settingService;
    private final MerchantService merchantService;
    // private final RestTemplate restTemplate; // Unused field

    @GetMapping("")
    public String showSettlements(Model model, HttpServletRequest request) {
        List<Settlement> settlementList = settlementService.getAllSettlements();
        loadCurrencySetting(request);
        model.addAttribute("settlementList", settlementList);
        model.addAttribute("pageTitle", "All Settlements");

        return "settlements/settlements";
    }

    @GetMapping("/merchant/{uuid}")
    public String listAllSettlementsByMerchant(@PathVariable("uuid") String uuid, Model model) throws PaykeshoError {

        Merchant merchant = merchantService.getMerchantByUuid(uuid);

        List<Settlement> settlementList = settlementService.listAllByMerchant(merchant);
        model.addAttribute("settlementList", settlementList);
        model.addAttribute("pageTitle", "Merchant Settlements");

        return "settlements/settlements";
    }

    // Merchant payments settle one by one through Lenco when the client pays
    // (payment service), so there is no batch to run here. This used to redirect
    // to itself forever.
    @GetMapping("/process-settlements")
    public String triggerSettlementProcessing(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message",
                "Merchant payments settle automatically, one by one, when each client payment is made.");
        return "redirect:/settlements";
    }

/*    @Async
    public void processSettlementAsync() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "http://localhost:8084/rest/api/settlements/process-settlements",
                    String.class
            );
            log.info("Settlement API response: {}", response.getBody());
        } catch (Exception e) {
            log.error("Error processing settlements: {}", e.getMessage(), e);
        }
        CompletableFuture.completedFuture(null);
    }*/

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();
        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
