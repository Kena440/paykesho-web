package org.paykesho.backoffice.settlement.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.dto.CardPaymentInitialResponse;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.Settlement;
import org.commons.entity.settings.Setting;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.merchants.service.MerchantService;
import org.paykesho.backoffice.settings.service.SettingService;
import org.paykesho.backoffice.settlement.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate;

    @GetMapping("")
    public String showSettlements(Model model, HttpServletRequest request) {
        List<Settlement> settlementList = settlementService.getAllSettlements();
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

    @PostMapping("/process-settlements")
    public String processSettlement() throws PaykeshoError {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:8084/rest/api/settlements/process-settlements", null, String.class);
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Payment API call failed: Status Code {}, Response {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaykeshoError("Payment initialization failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during payment initialization", e);
            throw new PaykeshoError("Unexpected error during payment initialization.");
        }
    }

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();
        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }
}
