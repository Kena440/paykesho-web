package org.paykesho.backoffice.merchantadmin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.commons.entity.merchant.MerchantAdmin;
import org.commons.entity.settings.Country;
import org.commons.enums.AuthenticationType;
import org.commons.enums.NotificationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.merchantadmin.repository.MerchantAdminRepository;
import org.paykesho.backoffice.notification.general.NotificationClient;
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
public class MerchantAdminService {
    public static final int MEMBER_ADMINS_PER_PAGE = 20;
    private static final Logger LOGGER = LoggerFactory.getLogger(MerchantAdminService.class);
    private final MerchantAdminRepository merchantAdminRepository;
    private final CountryRepository countryRepository;
    private final SettingService settingService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationClient notificationClient;

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws PaykeshoError {
        helper.listEntities(pageNum, MEMBER_ADMINS_PER_PAGE, merchantAdminRepository);
    }

    public void updateCustomerEnabledStatus(Integer id, boolean enabled) {
        merchantAdminRepository.updateEnabledStatus(id, enabled);
    }

    public MerchantAdmin get(Integer id) throws PaykeshoError {
        try {
            return merchantAdminRepository.findById(id).get();
        } catch (Exception exception) {
            throw new PaykeshoError("no customer found " + exception);
        }
    }

    public List<Country> countryList() {
        return countryRepository.findAllByOrderByNameDesc();
    }

    public boolean isEmailUnique(Integer id, String email) {
        MerchantAdmin existingMerchantAdmin = merchantAdminRepository.findByEmail(email);

        if (existingMerchantAdmin != null && existingMerchantAdmin.getId() != id) {
            return false;
        }

        return true;
    }

    public MerchantAdmin save(HttpServletRequest request, MerchantAdmin merchantAdmin) throws MessagingException, UnsupportedEncodingException {
        MerchantAdmin merchantAdminInDB = merchantAdminRepository.findByEmail(merchantAdmin.getEmail());

        if (merchantAdminInDB == null) {

            String token = RandomString.make(30);
            String passwordCode = RandomString.make(10);
            String verificationCode = RandomString.make(64);

            String encodedPassword = passwordEncoder.encode(passwordCode);
            merchantAdmin.setPassword(encodedPassword);

            merchantAdmin.setVerificationCode(verificationCode);
            merchantAdmin.setCreatedTime(new Date());
            merchantAdmin.setEnabled(true);
            merchantAdmin.setAuthenticationType(AuthenticationType.DATABASE);
            merchantAdmin.setResetPasswordToken(token);

            new Thread(() -> {
                try {
                    sendNotification(merchantAdmin, passwordCode);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }).start();

        } else {
            merchantAdmin.setResetPasswordToken(merchantAdminInDB.getResetPasswordToken());
            merchantAdmin.setAuthenticationType(merchantAdminInDB.getAuthenticationType());
            merchantAdmin.setPassword(merchantAdminInDB.getPassword());
            merchantAdmin.setVerificationCode(merchantAdminInDB.getVerificationCode());
        }

        return merchantAdminRepository.save(merchantAdmin);
    }

    public Long countAdmins() {
        return merchantAdminRepository.count();
    }

    private void sendNotification(MerchantAdmin merchantAdmin, String passwordCode) {
        // Generate messages

        String emailMessage = generateEmailMessage(merchantAdmin, passwordCode);
        String smsMessage = generateSmsMessage(merchantAdmin, passwordCode);


        // Send email notification
        boolean emailSent = notificationClient.sendNotification(merchantAdmin.getEmail(), emailMessage, NotificationType.email.defaultDescription());
        if (emailSent) {
            log.info("Email sent successfully to {}", merchantAdmin.getEmail());
        } else {
            log.error("Failed to send email to {}", merchantAdmin.getEmail());
        }

        // Send SMS notification
        boolean smsSent = notificationClient.sendNotification(merchantAdmin.getPhoneNumber(), smsMessage, NotificationType.sms.defaultDescription());
        if (smsSent) {
            log.info("SMS sent successfully to {}", merchantAdmin.getPhoneNumber());
        } else {
            log.error("Failed to send SMS to {}", merchantAdmin.getPhoneNumber());
        }
    }


    private String generateEmailMessage(MerchantAdmin merchantAdmin, String passwordCode) {
        StringBuilder message = new StringBuilder("<p>Greetings ").append(merchantAdmin.getFullName()).append(",</p>");

        if ("REGISTRATION".equals("REGISTRATION")) {
            message.append("<p>You have been registered as an admin. ").append("URL: ").append("https://merchant.paykesho.com/").append("Email: ").append(merchantAdmin.getEmail()).append("Password: ").append(passwordCode).append(".</p>");
        } else {
            message.append("<p>Unknown action type.</p>");
        }

        message.append("<br><p>Thank you,</p><p>Paykesho Team</p>");
        return message.toString();
    }


    private String generateSmsMessage(MerchantAdmin merchantAdmin, String passwordCode) {
        if ("REGISTRATION".equals("REGISTRATION")) {
            return String.format("Greetings %s,You have been registered as an admin. URL: %s. Email: %s. Password: %s. Thank you. Get lelo, Pay Kesho.", "https://merchant.paykesho.com/", merchantAdmin.getEmail(), passwordCode);
        }
        return "Unknown action type.";
    }
}
