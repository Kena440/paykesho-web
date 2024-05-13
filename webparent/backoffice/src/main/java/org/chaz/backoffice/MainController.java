package org.chaz.backoffice;

import org.chaz.backoffice.config.Utility;
import org.chaz.backoffice.donations.service.DonationService;
import org.chaz.backoffice.events.service.EventsService;
import org.chaz.backoffice.finance.service.InvoiceService;
import org.chaz.backoffice.iga.service.IGAService;
import org.chaz.backoffice.memberadmin.service.MemberAdminService;
import org.chaz.backoffice.memberinstitution.service.MemberInstitutionService;
import org.chaz.backoffice.settings.email.EmailSettingBag;
import org.chaz.backoffice.settings.service.SettingService;
import org.membershipcommons.entity.settings.Country;
import org.membershipcommons.entity.settings.Setting;
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
    private MemberAdminService memberAdminService;
    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private DonationService donationService;
    @Autowired
    private MemberInstitutionService memberInstitutionService;
    @Autowired
    private IGAService igaService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private EventsService eventsService;

    @GetMapping("")
    public String viewHomePage(Model model, HttpServletRequest request) {
        Country country = settingService.getCountryByName("Zambia");

        long totalLoans = igaService.getTotalMemberIGALoans();
        long totalOwingLoans = igaService.getCountOwingMemberIGALoans(false);
        Double totalOwing = igaService.getTotalOwingMemberIGALoans(false);

        loadCurrencySetting(request);

        model.addAttribute("totalOwing", totalOwing);
        model.addAttribute("totalOwingLoans", totalOwingLoans);
        model.addAttribute("totalLoans", totalLoans);
        model.addAttribute("countUnpaidInvoices", invoiceService.getCountMemberInstitutionsInvoice(true, false));
        model.addAttribute("countUnconfirmedDonations", donationService.getCountMemberInstitutionsDonation(true, false));
        model.addAttribute("countTotalDonations", donationService.getCount());
        model.addAttribute("memberAdminCount", memberAdminService.countAdmins());
        model.addAttribute("denominationCount", settingService.countDenominations());
        model.addAttribute("memberInstitutionCount", memberInstitutionService.countInstitutions());
        model.addAttribute("memberInstitutionCatchment", memberInstitutionService.getTotalCatchmentMembers(true));
        model.addAttribute("upcomingEvents", eventsService.getUpcomingEvents(30));
        if (country != null) model.addAttribute("country", country);
        if (country != null)
            model.addAttribute("memberCountByProvince", memberInstitutionService.getMembersCountByProvince(country.getId(), true));
        if (country != null)
            model.addAttribute("memberCountByDenomination", memberInstitutionService.getMembersCountByDenomination(country.getId(), true));
        if (country != null)
            model.addAttribute("memberCountByLevelOfCare", memberInstitutionService.getMembersCountByLevelOfCare(country.getId(), true));
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
        String subject = emailSettingBag.getMemberAdminCreationSubject();
        String content = emailSettingBag.getMemberAdminCreationContent();
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
