package org.chaz.csoportal;

import org.chaz.csoportal.csoadmins.service.CSOAdminService;
import org.chaz.csoportal.events.service.EventsService;
import org.chaz.csoportal.facilities.service.FacilityService;
import org.chaz.csoportal.settings.service.SettingService;
import org.commons.entity.cso.CSOAdmin;
import org.commons.entity.settings.Country;
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
    private CSOAdminService csoAdminService;
    @Autowired
    private FacilityService facilityService;
    @Autowired
    private EventsService eventsService;
    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String viewHomePage(Model model, HttpServletRequest request) {
        CSOAdmin csoAdmin = csoAdminService.getAuthenticatedCSOAdmin(request);
        Country country = settingService.getCountryByName("Zambia");

        int countTotalInterviews = 0;
        int countTotalAssessments = 0;
        model.addAttribute("upcomingEvents", eventsService.getUpcomingEvents(30));
        model.addAttribute("countTotalInterviews", countTotalInterviews);
        model.addAttribute("countTotalAssessments", countTotalAssessments);
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
