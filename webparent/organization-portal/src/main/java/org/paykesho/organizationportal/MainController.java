package org.paykesho.organizationportal;

import org.commons.entity.organization.Organization;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.entity.settings.Country;
import org.paykesho.organizationportal.organizationadmins.service.OrganizationAdminService;
import org.paykesho.organizationportal.organizations.service.OrganizationService;
import org.paykesho.organizationportal.settings.service.SettingService;
import org.paykesho.organizationportal.vouchers.service.VouchersService;
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

    @Autowired
    private OrganizationAdminService organizationAdminService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private VouchersService voucherService;
    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String viewHomePage(Model model, HttpServletRequest request) {
        OrganizationAdmin organizationAdmin = organizationAdminService.getAuthenticatedOrganizationAdmin(request);
        Organization organization = organizationService.getOrganizationByOrganizationAdmin(organizationAdmin);

        Country country = settingService.getCountryByName("Zambia");
        Map<String, Object> voucherStats = voucherService.getVoucherStats();
        model.addAttribute("totalVouchers", voucherStats.get("totalVouchers"));
        model.addAttribute("voucherPercentageChange", voucherStats.get("voucherPercentageChange"));
        model.addAttribute("organization", organization);
        model.addAttribute("organizationAdmin", organizationAdmin);
        model.addAttribute("pageTitle", "Dashboard");
        if (country != null) model.addAttribute("country", country);

        return "index";
    }

    @GetMapping("/login")
    public String viewLoginPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "security/login";
        }
        return "redirect:/organizations/dashboard";
    }

    @GetMapping("/sign-up")
    public String viewSignUpPage(Model model) {
        return "register/signup_home";
    }
}
