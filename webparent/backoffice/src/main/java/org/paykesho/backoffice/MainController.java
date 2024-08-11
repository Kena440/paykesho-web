package org.paykesho.backoffice;

import org.commons.entity.settings.Country;
import org.commons.entity.settings.Setting;
import org.paykesho.backoffice.config.Utility;
import org.paykesho.backoffice.events.service.EventsService;
import org.paykesho.backoffice.organizations.service.OrganizationService;
import org.paykesho.backoffice.settings.email.EmailSettingBag;
import org.paykesho.backoffice.settings.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.List;

@Controller
public class MainController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);
    @Autowired
    private SettingService settingService;
    @Autowired
    private EventsService eventsService;
    @Autowired
    private OrganizationService facilityService;

    @GetMapping("")
    public String viewHomePage(Model model, HttpServletRequest request) {
        Country country = settingService.getCountryByName("Zambia");

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
