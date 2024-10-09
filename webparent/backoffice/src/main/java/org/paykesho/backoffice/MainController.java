package org.paykesho.backoffice;

import org.commons.entity.settings.Country;
import org.paykesho.backoffice.events.service.EventsService;
import org.paykesho.backoffice.organizations.service.OrganizationService;
import org.paykesho.backoffice.settings.service.SettingService;
import org.paykesho.backoffice.transaction.service.TransactionsService;
import org.paykesho.backoffice.voucher.service.VoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class MainController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);
    @Autowired
    private SettingService settingService;
    @Autowired
    private EventsService eventsService;
    @Autowired
    private OrganizationService facilityService;
    @Autowired
    private TransactionsService transactionsService;
    @Autowired
    private VoucherService voucherService;

    @GetMapping("")
    public String viewHomePage(Model model, HttpServletRequest request) {
        Country country = settingService.getCountryByName("Zambia");
        Map<String, Object> txnStats = transactionsService.getTransactionStats();
        Map<String, Object> voucherStats = voucherService.getVoucherStats();
        model.addAttribute("totalTransactions", txnStats.get("totalTransactions"));
        model.addAttribute("totalVouchers", voucherStats.get("totalVouchers"));
        model.addAttribute("txnPercentageChange", txnStats.get("txnPercentageChange"));
        model.addAttribute("voucherPercentageChange", voucherStats.get("voucherPercentageChange"));
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("facilityCount", facilityService.countInstitutions());
        model.addAttribute("upcomingEvents", eventsService.getUpcomingEvents(30));
        if (country != null) model.addAttribute("country", country);
        return "index";
    }

    @GetMapping("/login")
    public String viewLoginPage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "security/login";
        }

        return "redirect:/";
    }


}
