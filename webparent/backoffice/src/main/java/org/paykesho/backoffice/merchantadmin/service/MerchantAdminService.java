package org.paykesho.backoffice.merchantadmin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.MerchantAdmin;
import org.commons.entity.settings.Country;
import org.commons.enums.AuthenticationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.merchantadmin.repository.MerchantAdminRepository;
import org.paykesho.backoffice.notification.general.NotificationClient;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.settings.repository.CountryRepository;
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
    // private final SettingService settingService; // Unused field
    private final PasswordEncoder passwordEncoder;
    private final NotificationClient notificationClient;

    // Welcome emails/SMS carry the new admin's password. Only queue them when the
    // notification service is actually delivering; otherwise the password would sit
    // in the queue table unsent and the back office shows it once instead.
    @org.springframework.beans.factory.annotation.Value("${paykesho.notifications.delivery-enabled:false}")
    private boolean notificationDeliveryEnabled;

    public boolean isNotificationDeliveryEnabled() {
        return notificationDeliveryEnabled;
    }

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

    public MerchantAdmin getByUuid(String uuid) throws PaykeshoError {
        try {
            return merchantAdminRepository.findByUuid(uuid)
                    .orElseThrow(() -> new PaykeshoError("Merchant admin not found with UUID: " + uuid));
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

            LOGGER.info("Generated password for new merchant admin: {}", securePassword);
            LOGGER.info("Generated token: {}", token);
            LOGGER.info("Generated verification code: {}", verificationCode);

            String encodedPassword = passwordEncoder.encode(securePassword);
            merchantAdmin.setPassword(encodedPassword);

            merchantAdmin.setVerificationCode(verificationCode);
            merchantAdmin.setCreatedTime(new Date());
            merchantAdmin.setEnabled(true);
            merchantAdmin.setAuthenticationType(AuthenticationType.DATABASE);
            merchantAdmin.setResetPasswordToken(token);
            merchantAdmin.setFailedAttempt(0);
            merchantAdmin.setAccountNonLocked(true);

            LOGGER.info("Saving new merchant admin to database: {}", merchantAdmin.getEmail());
            MerchantAdmin savedMerchantAdmin;
            try {
                savedMerchantAdmin = merchantAdminRepository.save(merchantAdmin);
                LOGGER.info("Successfully saved merchant admin with ID: {}", savedMerchantAdmin.getId());
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("Duplicate entry") && e.getMessage().contains("email")) {
                    LOGGER.error("Duplicate email error when saving merchant admin: {}", merchantAdmin.getEmail());
                    throw new RuntimeException("A merchant admin with email '" + merchantAdmin.getEmail() + "' already exists. Please use a different email address.");
                }
                throw e;
            }

            // Send notifications asynchronously after successful save
            new Thread(() -> {
                try {
                    sendWelcomeNotification(savedMerchantAdmin, securePassword, merchant);
                } catch (Exception e) {
                    LOGGER.error("Failed to send welcome notification: {}", e.getMessage());
                }
            }).start();

            return savedMerchantAdmin;

        } else {
            // Update existing admin - preserve existing credentials
            merchantAdmin.setId(merchantAdminInDB.getId()); // Set the ID to indicate this is an update
            merchantAdmin.setResetPasswordToken(merchantAdminInDB.getResetPasswordToken());
            merchantAdmin.setAuthenticationType(merchantAdminInDB.getAuthenticationType());
            merchantAdmin.setPassword(merchantAdminInDB.getPassword());
            merchantAdmin.setVerificationCode(merchantAdminInDB.getVerificationCode());
            merchantAdmin.setCreatedTime(merchantAdminInDB.getCreatedTime());
            merchantAdmin.setEnabled(merchantAdminInDB.isEnabled());
            merchantAdmin.setFailedAttempt(merchantAdminInDB.getFailedAttempt());
            merchantAdmin.setAccountNonLocked(merchantAdminInDB.isAccountNonLocked());

            LOGGER.info("Updating existing merchant admin: {}", merchantAdmin.getEmail());
            return merchantAdminRepository.save(merchantAdmin);
        }
    }

    public AdminSaveResult saveWithResult(HttpServletRequest request, MerchantAdmin merchantAdmin, Merchant merchant) throws MessagingException, UnsupportedEncodingException {
        MerchantAdmin merchantAdminInDB = merchantAdminRepository.findByEmail(merchantAdmin.getEmail());

        if (merchantAdminInDB == null) {
            // Generate secure password and tokens
            String securePassword = generateSecurePassword();
            String token = RandomString.make(30);
            String verificationCode = RandomString.make(64);

            LOGGER.info("Generated password for new merchant admin: {}", securePassword);
            LOGGER.info("Generated token: {}", token);
            LOGGER.info("Generated verification code: {}", verificationCode);

            // Set admin properties
            merchantAdmin.setPassword(passwordEncoder.encode(securePassword));
            merchantAdmin.setVerificationCode(verificationCode);
            merchantAdmin.setAuthenticationType(AuthenticationType.DATABASE);
            merchantAdmin.setResetPasswordToken(token);
            merchantAdmin.setFailedAttempt(0);
            merchantAdmin.setAccountNonLocked(true);

            LOGGER.info("Saving new merchant admin to database: {}", merchantAdmin.getEmail());
            MerchantAdmin savedMerchantAdmin;
            try {
                savedMerchantAdmin = merchantAdminRepository.save(merchantAdmin);
                LOGGER.info("Successfully saved merchant admin with ID: {}", savedMerchantAdmin.getId());
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("Duplicate entry") && e.getMessage().contains("email")) {
                    LOGGER.error("Duplicate email error when saving merchant admin: {}", merchantAdmin.getEmail());
                    throw new RuntimeException("A merchant admin with email '" + merchantAdmin.getEmail() + "' already exists. Please use a different email address.");
                }
                throw e;
            }

            // Send notifications asynchronously and track success
            boolean notificationsSent = notificationDeliveryEnabled && sendWelcomeNotificationWithResult(savedMerchantAdmin, securePassword, merchant);

            return new AdminSaveResult(savedMerchantAdmin, securePassword, notificationsSent);
        } else {
            // Update existing admin
            merchantAdminInDB.setName(merchantAdmin.getName());
            merchantAdminInDB.setTitle(merchantAdmin.getTitle());
            merchantAdminInDB.setPhoneNumber(merchantAdmin.getPhoneNumber());
            merchantAdminInDB.setEnabled(merchantAdmin.isEnabled());
            merchantAdminInDB.setAuthenticationType(merchantAdmin.getAuthenticationType());
            merchantAdminInDB.setResetPasswordToken(merchantAdmin.getResetPasswordToken());
            merchantAdminInDB.setFailedAttempt(merchantAdmin.getFailedAttempt());
            merchantAdminInDB.setAccountNonLocked(merchantAdmin.isAccountNonLocked());
            merchantAdminInDB.setVerificationCode(merchantAdmin.getVerificationCode());
            merchantAdminInDB.setPassword(merchantAdmin.getPassword());
            merchantAdminInDB.setId(merchantAdminInDB.getId()); // Ensure ID is set for update

            LOGGER.info("Updating existing merchant admin: {}", merchantAdminInDB.getEmail());
            MerchantAdmin savedMerchantAdmin = merchantAdminRepository.save(merchantAdminInDB);
            LOGGER.info("Successfully updated merchant admin with ID: {}", savedMerchantAdmin.getId());

            return new AdminSaveResult(savedMerchantAdmin, null, true); // No password for updates
        }
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
        java.security.SecureRandom random = new java.security.SecureRandom();

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
                log.warn("Admin was created successfully but welcome email could not be sent. Please check notification service availability.");
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
                    log.warn("Admin was created successfully but welcome SMS could not be sent. Please check notification service availability.");
                }
            }
        } catch (Exception e) {
            log.error("Error sending welcome notification: {}", e.getMessage());
        }
    }

    private boolean sendWelcomeNotificationWithResult(MerchantAdmin merchantAdmin, String password, Merchant merchant) {
        try {
            // Use the new templated email service
            boolean emailSent = notificationClient.sendMerchantAdminWelcomeEmail(
                    merchantAdmin.getFullName(),
                    merchantAdmin.getEmail(),
                    merchant.getName(),
                    "https://merchant.paykesho.com/",
                    password
            );

            boolean smsSent = true; // Default to true if no phone number
            if (merchantAdmin.getPhoneNumber() != null && !merchantAdmin.getPhoneNumber().trim().isEmpty()) {
                String smsMessage = String.format("Welcome to Paykesho! You're now a merchant admin for %s. Login: https://merchant.paykesho.com/ Email: %s Password: %s. Change password after first login. Contact support if needed.",
                        merchant.getName(), merchantAdmin.getEmail(), password);

                smsSent = notificationClient.sendNotification(merchantAdmin.getPhoneNumber(), smsMessage, "sms");
            }

            boolean allNotificationsSent = emailSent && smsSent;

            if (allNotificationsSent) {
                log.info("All welcome notifications sent successfully to {} for merchant {}", merchantAdmin.getEmail(), merchant.getName());
            } else {
                log.warn("Some welcome notifications failed for {} - Email: {}, SMS: {}",
                        merchantAdmin.getEmail(), emailSent, smsSent);
            }

            return allNotificationsSent;
        } catch (Exception e) {
            log.error("Error sending welcome notification: {}", e.getMessage());
            return false;
        }
    }


    public String resetPassword(MerchantAdmin merchantAdmin, Merchant merchant) throws PaykeshoError {
        try {
            log.debug("Resetting password for merchant admin: {}", merchantAdmin.getEmail());

            // Generate new temporary password
            String newPassword = generateTemporaryPassword();

            // Encode and update password
            String encodedPassword = passwordEncoder.encode(newPassword);
            merchantAdmin.setPassword(encodedPassword);
            merchantAdmin.setAccountNonLocked(true);
            merchantAdmin.setFailedAttempt(0);
            merchantAdmin.setEnabled(true); // Enable account after password reset

            // Save updated admin
            merchantAdminRepository.save(merchantAdmin);

            // Send password reset notification
            if (notificationDeliveryEnabled) sendPasswordResetNotification(merchantAdmin, newPassword, merchant);

            log.info("Password reset successful for merchant admin: {}", merchantAdmin.getEmail());
            return newPassword;

        } catch (Exception e) {
            log.error("Error resetting password for merchant admin {}: {}", merchantAdmin.getEmail(), e.getMessage(), e);
            throw new PaykeshoError("Failed to reset password: " + e.getMessage());
        }
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder password = new StringBuilder();

        // Ensure at least one character from each category
        password.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(random.nextInt(26))); // Uppercase
        password.append("abcdefghijklmnopqrstuvwxyz".charAt(random.nextInt(26))); // Lowercase
        password.append("0123456789".charAt(random.nextInt(10))); // Number
        password.append("!@#$%^&*".charAt(random.nextInt(8))); // Special character

        // Fill remaining characters
        for (int i = 4; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
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

    private void sendPasswordResetNotification(MerchantAdmin merchantAdmin, String newPassword, Merchant merchant) {
        try {
            log.debug("Sending password reset notification to: {}", merchantAdmin.getEmail());
            // Note: Password is not logged for security reasons

            // Send email notification using professional template with new password
            boolean emailSent = notificationClient.sendAdminPasswordResetEmail(
                merchantAdmin.getFullName(),
                merchantAdmin.getEmail(),
                "https://merchant.paykesho.com/",
                newPassword,
                merchant.getName()
            );

            if (emailSent) {
                log.info("Password reset email sent successfully to {}", merchantAdmin.getEmail());
            } else {
                log.error("Failed to send password reset email to {}", merchantAdmin.getEmail());
            }

            // Send SMS notification if phone number is available
            if (merchantAdmin.getPhoneNumber() != null && !merchantAdmin.getPhoneNumber().trim().isEmpty()) {
                String smsMessage = generatePasswordResetSmsMessage(merchantAdmin, newPassword, merchant);
                boolean smsSent = notificationClient.sendNotification(merchantAdmin.getPhoneNumber(), smsMessage, "sms");

                if (smsSent) {
                    log.info("Password reset SMS sent successfully to {}", merchantAdmin.getPhoneNumber());
                } else {
                    log.error("Failed to send password reset SMS to {}", merchantAdmin.getPhoneNumber());
                }
            }
        } catch (Exception e) {
            log.error("Error sending password reset notification: {}", e.getMessage());
        }
    }


    private String generatePasswordResetSmsMessage(MerchantAdmin merchantAdmin, String newPassword, Merchant merchant) {
        return String.format("Paykesho Password Reset: Your password has been reset for %s. New password: %s. Login: https://merchant.paykesho.com/ Change password immediately after login. Contact support if you didn't request this.",
                merchant.getName(), newPassword);
    }

    public static class AdminSaveResult {
        private final MerchantAdmin admin;
        private final String password;
        private final boolean notificationsSent;

        public AdminSaveResult(MerchantAdmin admin, String password, boolean notificationsSent) {
            this.admin = admin;
            this.password = password;
            this.notificationsSent = notificationsSent;
        }

        public MerchantAdmin getAdmin() {
            return admin;
        }

        public String getPassword() {
            return password;
        }

        public boolean isNotificationsSent() {
            return notificationsSent;
        }
    }
}
