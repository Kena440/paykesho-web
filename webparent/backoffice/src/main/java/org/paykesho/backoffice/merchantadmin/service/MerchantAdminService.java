package org.paykesho.backoffice.merchantadmin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.commons.entity.merchant.Merchant;
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

    public MerchantAdmin save(HttpServletRequest request, MerchantAdmin merchantAdmin, Merchant merchant) throws MessagingException, UnsupportedEncodingException {
        MerchantAdmin merchantAdminInDB = merchantAdminRepository.findByEmail(merchantAdmin.getEmail());

        if (merchantAdminInDB == null) {

            String token = RandomString.make(30);
            String passwordCode = merchant.getCode();
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
        // Fetch email settings
        var emailSettingBags = settingService.getEmailSettingsBag();
        String fromAddress = emailSettingBags.getFromAddress();
        String senderName = emailSettingBags.getSenderName();

        // Generate messages
        String emailMessage = generateEmailMessage(merchantAdmin, passwordCode);
        String smsMessage = generateSmsMessage(merchantAdmin, passwordCode);

        // Send email notification
        boolean emailSent = notificationClient.sendNotification(merchantAdmin.getEmail(), emailMessage, NotificationType.email.defaultDescription());
        if (emailSent) {
            log.info("Email sent successfully to {} from {} ({})", merchantAdmin.getEmail(), senderName, fromAddress);
        } else {
            log.error("Failed to send email to {} from {} ({})", merchantAdmin.getEmail(), senderName, fromAddress);
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
        StringBuilder message = new StringBuilder();
        message.append("<p>Hello ").append(merchantAdmin.getFullName()).append(",</p>");
        message.append("<p>You have been registered as a merchant admin.</p>");
        message.append("<p><b>Login URL:</b> <a href=\"https://merchant.paykesho.com/\">https://merchant.paykesho.com/</a></p>");
        message.append("<p><b>Email:</b> ").append(merchantAdmin.getEmail()).append("</p>");
        message.append("<p><b>Password:</b> ").append(passwordCode).append("</p>");
        message.append("<br><p>Thank you,<br>Paykesho Team</p>");
        return message.toString();
    }

    private String generateSmsMessage(MerchantAdmin merchantAdmin, String passwordCode) {
        return String.format("Hello %s, You have been registered as a merchant admin. Login: https://merchant.paykesho.com/ Email: %s Password: %s. Thank you. Paykesho Team.", merchantAdmin.getFullName(), merchantAdmin.getEmail(), passwordCode);
    }
}
