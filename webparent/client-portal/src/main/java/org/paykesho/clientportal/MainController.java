package org.paykesho.clientportal;

import org.commons.entity.client.Client;
import org.commons.entity.organization.Organization;
import org.commons.entity.settings.Country;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.events.service.EventsService;
import org.paykesho.clientportal.merchant.service.MerchantService;
import org.paykesho.clientportal.organizations.service.OrganizationService;
import org.paykesho.clientportal.settings.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class MainController {

    @Autowired
    private ClientService clientService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private MerchantService merchantService;
    @Autowired
    private EventsService eventsService;
    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String viewHomePage(Model model) {
        Country country = settingService.getCountryByName("Zambia");
        model.addAttribute("upcomingEvents", eventsService.getUpcomingEvents(30));

        if (country != null) model.addAttribute("country", country);

        return "index";
    }

    @GetMapping("/faq")
    public String viewFAQPage(Model model) {
        Country country = settingService.getCountryByName("Zambia");
        model.addAttribute("upcomingEvents", eventsService.getUpcomingEvents(30));

        if (country != null) model.addAttribute("country", country);

        return "faq";
    }

    @GetMapping("/login")
    public String viewLoginPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            List<Country> countryList = clientService.countryList();
            List<Organization> corporateList = organizationService.organizationListEnabled();

            model.addAttribute("countryList", countryList);
            model.addAttribute("corporateList", corporateList);
            model.addAttribute("pageTitle", "Sign Up");
            model.addAttribute("customer", new Client());
            return "security/login";
        }
        return "redirect:/clients/dashboard";
    }

    @GetMapping("/sign-up")
    public String viewSignUpPage(Model model) {
        return "register/signup_home";
    }
}
