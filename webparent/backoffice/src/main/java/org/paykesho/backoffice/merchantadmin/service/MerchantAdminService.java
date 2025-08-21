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
import java.util.Random;

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
            throw new PaykeshoError("Merchant admin not found: " + exception.getMessage());
        }
    }

    public MerchantAdmin getByEmail(String email) {
        return merchantAdminRepository.findByEmail(email);
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
            // Generate secure password and tokens
            String securePassword = generateSecurePassword();
            String token = RandomString.make(30);
            String verificationCode = RandomString.make(64);

            String encodedPassword = passwordEncoder.encode(securePassword);
            merchantAdmin.setPassword(encodedPassword);

            merchantAdmin.setVerificationCode(verificationCode);
            merchantAdmin.setCreatedTime(new Date());
            merchantAdmin.setEnabled(true);
            merchantAdmin.setAuthenticationType(AuthenticationType.DATABASE);
            merchantAdmin.setResetPasswordToken(token);
            merchantAdmin.setFailedAttempt(0);
            merchantAdmin.setAccountNonLocked(true);

            // Send notifications asynchronously
            new Thread(() -> {
                try {
                    sendWelcomeNotification(merchantAdmin, securePassword, merchant);
                } catch (Exception e) {
                    LOGGER.error("Failed to send welcome notification: {}", e.getMessage());
                }
            }).start();

        } else {
            // Update existing admin - preserve existing credentials
            merchantAdmin.setResetPasswordToken(merchantAdminInDB.getResetPasswordToken());
            merchantAdmin.setAuthenticationType(merchantAdminInDB.getAuthenticationType());
            merchantAdmin.setPassword(merchantAdminInDB.getPassword());
            merchantAdmin.setVerificationCode(merchantAdminInDB.getVerificationCode());
            merchantAdmin.setCreatedTime(merchantAdminInDB.getCreatedTime());
            merchantAdmin.setEnabled(merchantAdminInDB.isEnabled());
            merchantAdmin.setFailedAttempt(merchantAdminInDB.getFailedAttempt());
            merchantAdmin.setAccountNonLocked(merchantAdminInDB.isAccountNonLocked());
        }

        return merchantAdminRepository.save(merchantAdmin);
    }

    public Long countAdmins() {
        return merchantAdminRepository.count();
    }

    /**
     * Generate a secure password with complexity requirements
     */
    private String generateSecurePassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        
        // Ensure at least one character from each category
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));
        
        // Fill the rest with random characters
        String allChars = upperCase + lowerCase + numbers + specialChars;
        for (int i = 4; i < 12; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // Shuffle the password
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }

    private void sendWelcomeNotification(MerchantAdmin merchantAdmin, String password, Merchant merchant) {
        try {
            // Use the new templated email service
            boolean emailSent = notificationClient.sendMerchantAdminWelcomeEmail(
                merchantAdmin.getFullName(),
                merchantAdmin.getEmail(),
                merchant.getName(),
                "https://merchant.paykesho.com/",
                password
            );
            
            if (emailSent) {
                log.info("Welcome email sent successfully to {} for merchant {}", merchantAdmin.getEmail(), merchant.getName());
            } else {
                log.error("Failed to send welcome email to {} for merchant {}", merchantAdmin.getEmail(), merchant.getName());
            }

            // Send SMS notification if phone number is available
            if (merchantAdmin.getPhoneNumber() != null && !merchantAdmin.getPhoneNumber().trim().isEmpty()) {
                String smsMessage = String.format("Welcome to Paykesho! You're now a merchant admin for %s. Login: https://merchant.paykesho.com/ Email: %s Password: %s. Change password after first login. Contact support if needed.", 
                    merchant.getName(), merchantAdmin.getEmail(), password);
                
                boolean smsSent = notificationClient.sendNotification(merchantAdmin.getPhoneNumber(), smsMessage, "sms");
                if (smsSent) {
                    log.info("Welcome SMS sent successfully to {}", merchantAdmin.getPhoneNumber());
                } else {
                    log.error("Failed to send welcome SMS to {}", merchantAdmin.getPhoneNumber());
                }
            }
        } catch (Exception e) {
            log.error("Error sending welcome notification: {}", e.getMessage());
        }
    }

    private String generateWelcomeEmailMessage(MerchantAdmin merchantAdmin, String password, Merchant merchant) {
        StringBuilder message = new StringBuilder();
        message.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        message.append("<div style='background-color: #f8f9fa; padding: 20px; border-radius: 8px;'>");
        message.append("<h2 style='color: #007bff; margin-bottom: 20px;'>Welcome to Paykesho Merchant Portal!</h2>");
        message.append("<p>Hello <strong>").append(merchantAdmin.getFullName()).append("</strong>,</p>");
        message.append("<p>You have been successfully registered as a merchant admin for <strong>").append(merchant.getName()).append("</strong>.</p>");
        message.append("<div style='background-color: #e9ecef; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
        message.append("<h4 style='margin-top: 0; color: #495057;'>Your Login Credentials:</h4>");
        message.append("<p><strong>Login URL:</strong> <a href='https://merchant.paykesho.com/' style='color: #007bff;'>https://merchant.paykesho.com/</a></p>");
        message.append("<p><strong>Email:</strong> ").append(merchantAdmin.getEmail()).append("</p>");
        message.append("<p><strong>Temporary Password:</strong> <code style='background-color: #fff; padding: 2px 6px; border-radius: 3px;'>").append(password).append("</code></p>");
        message.append("</div>");
        message.append("<div style='background-color: #fff3cd; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #ffc107;'>");
        message.append("<h4 style='margin-top: 0; color: #856404;'>Security Recommendations:</h4>");
        message.append("<ul style='color: #856404;'>");
        message.append("<li>Change your password immediately after first login</li>");
        message.append("<li>Use a strong, unique password</li>");
        message.append("<li>Enable two-factor authentication if available</li>");
        message.append("<li>Never share your credentials with others</li>");
        message.append("</ul>");
        message.append("</div>");
        message.append("<p>If you have any questions or need assistance, please contact our support team.</p>");
        message.append("<p>Thank you,<br><strong>Paykesho Team</strong></p>");
        message.append("</div>");
        message.append("</div>");
        return message.toString();
    }

    private String generateWelcomeSmsMessage(MerchantAdmin merchantAdmin, String password, Merchant merchant) {
        return String.format("Welcome to Paykesho! You're now a merchant admin for %s. Login: https://merchant.paykesho.com/ Email: %s Password: %s. Change password after first login. Contact support if needed.", 
            merchant.getName(), merchantAdmin.getEmail(), password);
    }
}
