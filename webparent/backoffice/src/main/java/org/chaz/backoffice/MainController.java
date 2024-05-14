package org.chaz.backoffice;

import org.chaz.backoffice.assessments.service.AssessmentsService;
import org.chaz.backoffice.config.Utility;
import org.chaz.backoffice.events.service.EventsService;
import org.chaz.backoffice.facilities.service.FacilityService;
import org.chaz.backoffice.settings.email.EmailSettingBag;
import org.chaz.backoffice.settings.service.SettingService;
import org.commons.entity.settings.Country;
import org.commons.entity.settings.Setting;
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
    private AssessmentsService assessmentsService;
    @Autowired
    private FacilityService facilityService;

    @GetMapping("")
    public String viewHomePage(Model model, HttpServletRequest request) {
        Country country = settingService.getCountryByName("Zambia");
        loadCurrencySetting(request);

        model.addAttribute("facilityCount", facilityService.countInstitutions());
        model.addAttribute("upcomingEvents", eventsService.getUpcomingEvents(30));
        model.addAttribute("upcomingAssessments", assessmentsService.getUpcomingAssessments(30));
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

    //@GetMapping("/test-email")
    public String sendTestEmail(HttpServletRequest request) {
        new Thread(() -> {
            try {
                sendAdminCreationEmail(request);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }).start();
        return "empty_page";
    }

    private void sendAdminCreationEmail(HttpServletRequest request) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBag = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBag);

        mailSender.setDefaultEncoding("utf-8");

        String toAddress = "mwamba.kaenga@chaz.org.zm";
        String subject = emailSettingBag.getAssessorCreationSubject();
        String content = emailSettingBag.getAssessorCreationContent();
        String systemURL = Utility.getSiteUrl(request);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBag.getFromAddress(), emailSettingBag.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", "mwamba");
        content = content.replace("[[link]]", systemURL);
        content = content.replace("[[email]]", "kaengamwamba@gmail.com");
        content = content.replace("[[password]]", "12345");

        helper.setText(content, true);
        mailSender.send(mimeMessage);
    }

    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();

        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }

/*    private void loginNotification(Authentication authentication,
                                   HttpServletRequest request) {
        try {
            if (authentication.getPrincipal() instanceof MembershipMgtUserDetails) {
                deviceService.verifyDevice((((MembershipMgtUserDetails) authentication.getPrincipal()).getUser()), request);
            }
        } catch(Exception e) {
            LOGGER.error("An error occurred verifying device or location");
            throw new RuntimeException(e);
        }
    }*/
}
