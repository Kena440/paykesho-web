package org.paykesho.backoffice.organizationadmin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.commons.entity.organization.Organization;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.entity.settings.Country;
import org.commons.enums.AuthenticationType;
import org.commons.enums.NotificationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.notification.general.NotificationClient;
import org.paykesho.backoffice.organizationadmin.repository.OrganizationAdminRepository;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.settings.repository.CountryRepository;
import org.paykesho.backoffice.settings.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrganizationAdminService {
    public static final int MEMBER_ADMINS_PER_PAGE = 20;
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationAdminService.class);
    private final OrganizationAdminRepository organizationAdminRepository;
    private final CountryRepository countryRepository;
    private final SettingService settingService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationClient notificationClient;

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

    public OrganizationAdmin save(HttpServletRequest request, OrganizationAdmin organizationAdminInForm, Organization organization) throws MessagingException, UnsupportedEncodingException {
        OrganizationAdmin organizationAdminInDB = organizationAdminRepository.findByEmail(organizationAdminInForm.getEmail());

        if (organizationAdminInDB == null) {

            String token = RandomString.make(30);
            String passwordCode = organization.getPhoneNumber();
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
        // Fetch email settings
        var emailSettingBags = settingService.getEmailSettingsBag();
        String fromAddress = emailSettingBags.getFromAddress();
        String senderName = emailSettingBags.getSenderName();

        // Generate messages
        String emailMessage = generateEmailMessage(organizationAdmin, passwordCode);
        String smsMessage = generateSmsMessage(organizationAdmin, passwordCode);

        // Send email notification
        boolean emailSent = notificationClient.sendNotification(organizationAdmin.getEmail(), emailMessage, NotificationType.email.defaultDescription());
        if (emailSent) {
            log.info("Email sent successfully to {} from {} ({})", organizationAdmin.getEmail(), senderName, fromAddress);
        } else {
            log.error("Failed to send email to {} from {} ({})", organizationAdmin.getEmail(), senderName, fromAddress);
        }

        // Send SMS notification
        boolean smsSent = notificationClient.sendNotification(organizationAdmin.getPhoneNumber(), smsMessage, NotificationType.sms.defaultDescription());
        if (smsSent) {
            log.info("SMS sent successfully to {}", organizationAdmin.getPhoneNumber());
        } else {
            log.error("Failed to send SMS to {}", organizationAdmin.getPhoneNumber());
        }
    }

    private String generateEmailMessage(OrganizationAdmin organizationAdmin, String passwordCode) {
        StringBuilder message = new StringBuilder();
        message.append("<p>Hello ").append(organizationAdmin.getFullName()).append(",</p>");
        message.append("<p>You have been registered as an organization admin.</p>");
        message.append("<p><b>Login URL:</b> <a href=\"https://organization.paykesho.com/\">https://organization.paykesho.com/</a></p>");
        message.append("<p><b>Email:</b> ").append(organizationAdmin.getEmail()).append("</p>");
        message.append("<p><b>Password:</b> ").append(passwordCode).append("</p>");
        message.append("<br><p>Thank you,<br>Paykesho Team</p>");
        return message.toString();
    }

    private String generateSmsMessage(OrganizationAdmin organizationAdmin, String passwordCode) {
        return String.format("Hello %s, You have been registered as an organization admin. Login: https://organization.paykesho.com/ Email: %s Password: %s. Thank you. Paykesho Team.", organizationAdmin.getFullName(), organizationAdmin.getEmail(), passwordCode);
    }
}
