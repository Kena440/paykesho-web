package org.paykesho.backoffice.organizationadmin.service;

import net.bytebuddy.utility.RandomString;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.entity.settings.Country;
import org.commons.enums.AuthenticationType;
import org.commons.enums.NotificationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.notifications.service.NotificationService;
import org.paykesho.backoffice.organizationadmin.repository.OrganizationAdminRepository;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.settings.repository.CountryRepository;
import org.paykesho.backoffice.settings.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class OrganizationAdminService {
    public static final int MEMBER_ADMINS_PER_PAGE = 20;
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationAdminService.class);
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    NotificationService notificationService;
    @Autowired
    private OrganizationAdminRepository organizationAdminRepository;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private SettingService settingService;

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws PaykeshoError {
        helper.listEntities(pageNum, MEMBER_ADMINS_PER_PAGE, organizationAdminRepository);
    }

    public void updateCustomerEnabledStatus(Integer id, boolean enabled) {
        organizationAdminRepository.updateEnabledStatus(id, enabled);
    }

    public OrganizationAdmin get(Integer id) throws PaykeshoError {
        try {
            return organizationAdminRepository.findById(id).get();
        } catch (Exception exception) {
            throw new PaykeshoError("no customer found " + exception);
        }
    }

    public List<Country> countryList() {
        return countryRepository.findAllByOrderByNameDesc();
    }

    public boolean isEmailUnique(Integer id, String email) {
        OrganizationAdmin existingOrganizationAdmin = organizationAdminRepository.findByEmail(email);

        if (existingOrganizationAdmin != null && existingOrganizationAdmin.getId() != id) {
            return false;
        }

        return true;
    }

    public OrganizationAdmin save(HttpServletRequest request, OrganizationAdmin organizationAdminInForm) throws MessagingException, UnsupportedEncodingException {
        OrganizationAdmin organizationAdminInDB = organizationAdminRepository.findByEmail(organizationAdminInForm.getEmail());

        if (organizationAdminInDB == null) {

            String token = RandomString.make(30);
            String passwordCode = RandomString.make(10);
            String verificationCode = RandomString.make(64);

            String encodedPassword = passwordEncoder.encode(passwordCode);
            organizationAdminInForm.setPassword(encodedPassword);

            organizationAdminInForm.setVerificationCode(verificationCode);
            organizationAdminInForm.setCreatedTime(new Date());
            organizationAdminInForm.setEnabled(true);
            organizationAdminInForm.setAuthenticationType(AuthenticationType.DATABASE);
            organizationAdminInForm.setResetPasswordToken(token);

            new Thread(() -> {
                try {
                    sendNotification(organizationAdminInForm, passwordCode);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }).start();

        } else {
            organizationAdminInForm.setResetPasswordToken(organizationAdminInDB.getResetPasswordToken());
            organizationAdminInForm.setAuthenticationType(organizationAdminInDB.getAuthenticationType());
            organizationAdminInForm.setPassword(organizationAdminInDB.getPassword());
            organizationAdminInForm.setVerificationCode(organizationAdminInDB.getVerificationCode());
        }

        return organizationAdminRepository.save(organizationAdminInForm);
    }

    public void delete(Integer id) throws PaykeshoError {
        Long count = organizationAdminRepository.countById(id);
        if (count == null || count == 0) {
            throw new PaykeshoError("could not find customer");
        }
        organizationAdminRepository.deleteById(id);
    }

    public Long countAdmins() {
        return organizationAdminRepository.count();
    }

    private void sendNotification(OrganizationAdmin organizationAdmin, String passwordCode) {
        // Generate messages

        String emailMessage = generateEmailMessage(organizationAdmin, passwordCode);
        String smsMessage = generateSmsMessage(organizationAdmin, passwordCode);


        // Send email notification
        boolean emailSent = notificationService.sendNotification(organizationAdmin.getEmail(), emailMessage, NotificationType.email.defaultDescription());
        if (emailSent) {
            System.out.println("Email sent successfully to " + organizationAdmin.getEmail());
        } else {
            System.err.println("Failed to send email to " + organizationAdmin.getEmail());
        }

        // Send SMS notification
        boolean smsSent = notificationService.sendNotification(organizationAdmin.getPhoneNumber(), smsMessage, NotificationType.sms.defaultDescription());
        if (smsSent) {
            System.out.println("SMS sent successfully to " + organizationAdmin.getPhoneNumber());
        } else {
            System.err.println("Failed to send SMS to " + organizationAdmin.getPhoneNumber());
        }
    }


    private String generateEmailMessage(OrganizationAdmin organizationAdmin, String passwordCode) {
        StringBuilder message = new StringBuilder("<p>Greetings ").append(organizationAdmin.getFullName()).append(",</p>");

        if ("REGISTRATION".equals("REGISTRATION")) {
            message.append("<p>You have been registered as an admin. ").append("URL: ").append("https://merchant.paykesho.com/").append("Email: ").append(organizationAdmin.getEmail()).append("Password: ").append(passwordCode).append(".</p>");
        } else {
            message.append("<p>Unknown action type.</p>");
        }

        message.append("<br><p>Thank you,</p><p>Paykesho Team</p>");
        return message.toString();
    }

    private String generateSmsMessage(OrganizationAdmin organizationAdmin, String passwordCode) {
        if ("REGISTRATION".equals("REGISTRATION")) {
            return String.format("Greetings %s,You have been registered as an admin. URL: %s. Email: %s. Password: %s. Thank you. Get lelo, Pay Kesho.", "https://organization.paykesho.com/", organizationAdmin.getEmail(), passwordCode);
        }
        return "Unknown action type.";
    }
}
