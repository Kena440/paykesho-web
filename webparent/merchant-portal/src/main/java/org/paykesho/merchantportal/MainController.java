package org.paykesho.merchantportal;

import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.MerchantAdmin;
import org.commons.entity.settings.Country;
import org.paykesho.merchantportal.events.service.EventsService;
import org.paykesho.merchantportal.merchantadmins.service.MerchantAdminService;
import org.paykesho.merchantportal.merchants.service.MerchantService;
import org.paykesho.merchantportal.settings.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class MainController {

    @Autowired
    private MerchantAdminService merchantAdminService;
    @Autowired
    private MerchantService merchantService;
    @Autowired
    private EventsService eventsService;
    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String viewHomePage(Model model, HttpServletRequest request) {
        MerchantAdmin merchantAdmin = merchantAdminService.getAuthenticatedMerchantAdmin(request);
        Merchant merchant = merchantService.getMerchantByContactPerson(merchantAdmin);
        Country country = settingService.getCountryByName("Zambia");

        model.addAttribute("merchantAdmin", merchantAdmin);
        model.addAttribute("merchant", merchant);

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

    @GetMapping("/sign-up")
    public String viewSignUpPage(Model model) {
        return "register/signup_home";
    }
}
