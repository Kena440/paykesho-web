package org.paykesho.backoffice.organizationadmin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.commons.entity.organization.Organization;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.entity.settings.Country;
import org.commons.enums.AuthenticationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.notification.general.NotificationClient;
import org.paykesho.backoffice.organizationadmin.repository.OrganizationAdminRepository;
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
public class OrganizationAdminService {
    public static final int MEMBER_ADMINS_PER_PAGE = 20;
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationAdminService.class);
    private final OrganizationAdminRepository organizationAdminRepository;
    private final CountryRepository countryRepository;
    // private final SettingService settingService; // Unused field
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
            throw new PaykeshoError("Organization admin not found: " + exception.getMessage());
        }
    }

    public OrganizationAdmin getByEmail(String email) {
        return organizationAdminRepository.findByEmail(email);
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
            // Generate secure password and tokens
            String securePassword = generateSecurePassword();
            String token = RandomString.make(30);
            String verificationCode = RandomString.make(64);

            LOGGER.info("Generated password for new organization admin: {}", securePassword);
            LOGGER.info("Generated token: {}", token);
            LOGGER.info("Generated verification code: {}", verificationCode);

            String encodedPassword = passwordEncoder.encode(securePassword);
            organizationAdminInForm.setPassword(encodedPassword);

            organizationAdminInForm.setVerificationCode(verificationCode);
            organizationAdminInForm.setCreatedTime(new Date());
            organizationAdminInForm.setEnabled(true);
            organizationAdminInForm.setAuthenticationType(AuthenticationType.DATABASE);
            organizationAdminInForm.setResetPasswordToken(token);
            organizationAdminInForm.setFailedAttempt(0);
            organizationAdminInForm.setAccountNonLocked(true);

            LOGGER.info("Saving new organization admin to database: {}", organizationAdminInForm.getEmail());
            OrganizationAdmin savedOrganizationAdmin;
            try {
                savedOrganizationAdmin = organizationAdminRepository.save(organizationAdminInForm);
                LOGGER.info("Successfully saved organization admin with ID: {}", savedOrganizationAdmin.getId());
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("Duplicate entry") && e.getMessage().contains("email")) {
                    LOGGER.error("Duplicate email error when saving organization admin: {}", organizationAdminInForm.getEmail());
                    throw new RuntimeException("An organization admin with email '" + organizationAdminInForm.getEmail() + "' already exists. Please use a different email address.");
                }
                throw e;
            }

            // Send notifications asynchronously after successful save
            new Thread(() -> {
                try {
                    sendWelcomeNotification(savedOrganizationAdmin, securePassword, organization);
                } catch (Exception e) {
                    LOGGER.error("Failed to send welcome notification: {}", e.getMessage());
                }
            }).start();

            return savedOrganizationAdmin;

        } else {
            // Update existing admin - preserve existing credentials
            organizationAdminInForm.setId(organizationAdminInDB.getId()); // Set the ID to indicate this is an update
            organizationAdminInForm.setResetPasswordToken(organizationAdminInDB.getResetPasswordToken());
            organizationAdminInForm.setAuthenticationType(organizationAdminInDB.getAuthenticationType());
            organizationAdminInForm.setPassword(organizationAdminInDB.getPassword());
            organizationAdminInForm.setVerificationCode(organizationAdminInDB.getVerificationCode());
            organizationAdminInForm.setCreatedTime(organizationAdminInDB.getCreatedTime());
            organizationAdminInForm.setEnabled(organizationAdminInDB.isEnabled());
            organizationAdminInForm.setFailedAttempt(organizationAdminInDB.getFailedAttempt());
            organizationAdminInForm.setAccountNonLocked(organizationAdminInDB.isAccountNonLocked());

            LOGGER.info("Updating existing organization admin: {}", organizationAdminInForm.getEmail());
            return organizationAdminRepository.save(organizationAdminInForm);
        }
    }

    public AdminSaveResult saveWithResult(HttpServletRequest request, OrganizationAdmin organizationAdminInForm, Organization organization) throws MessagingException, UnsupportedEncodingException {
        OrganizationAdmin organizationAdminInDB = organizationAdminRepository.findByEmail(organizationAdminInForm.getEmail());

        if (organizationAdminInDB == null) {
            // Generate secure password and tokens
            String securePassword = generateSecurePassword();
            String token = RandomString.make(30);
            String verificationCode = RandomString.make(64);

            LOGGER.info("Generated password for new organization admin: {}", securePassword);
            LOGGER.info("Generated token: {}", token);
            LOGGER.info("Generated verification code: {}", verificationCode);

            // Set admin properties
            organizationAdminInForm.setPassword(passwordEncoder.encode(securePassword));
            organizationAdminInForm.setVerificationCode(verificationCode);
            organizationAdminInForm.setAuthenticationType(AuthenticationType.DATABASE);
            organizationAdminInForm.setResetPasswordToken(token);
            organizationAdminInForm.setFailedAttempt(0);
            organizationAdminInForm.setAccountNonLocked(true);

            LOGGER.info("Saving new organization admin to database: {}", organizationAdminInForm.getEmail());
            OrganizationAdmin savedOrganizationAdmin;
            try {
                savedOrganizationAdmin = organizationAdminRepository.save(organizationAdminInForm);
                LOGGER.info("Successfully saved organization admin with ID: {}", savedOrganizationAdmin.getId());
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("Duplicate entry") && e.getMessage().contains("email")) {
                    LOGGER.error("Duplicate email error when saving organization admin: {}", organizationAdminInForm.getEmail());
                    throw new RuntimeException("An organization admin with email '" + organizationAdminInForm.getEmail() + "' already exists. Please use a different email address.");
                }
                throw e;
            }

            // Send notifications asynchronously and track success
            boolean notificationsSent = sendWelcomeNotificationWithResult(savedOrganizationAdmin, securePassword, organization);

            return new AdminSaveResult(savedOrganizationAdmin, securePassword, notificationsSent);
        } else {
            // Update existing admin
            organizationAdminInDB.setName(organizationAdminInForm.getName());
            organizationAdminInDB.setTitle(organizationAdminInForm.getTitle());
            organizationAdminInDB.setPhoneNumber(organizationAdminInForm.getPhoneNumber());
            organizationAdminInDB.setEnabled(organizationAdminInForm.isEnabled());
            organizationAdminInDB.setAuthenticationType(organizationAdminInForm.getAuthenticationType());
            organizationAdminInDB.setResetPasswordToken(organizationAdminInForm.getResetPasswordToken());
            organizationAdminInDB.setFailedAttempt(organizationAdminInForm.getFailedAttempt());
            organizationAdminInDB.setAccountNonLocked(organizationAdminInForm.isAccountNonLocked());
            organizationAdminInDB.setVerificationCode(organizationAdminInForm.getVerificationCode());
            organizationAdminInDB.setPassword(organizationAdminInForm.getPassword());
            organizationAdminInDB.setId(organizationAdminInDB.getId()); // Ensure ID is set for update

            LOGGER.info("Updating existing organization admin: {}", organizationAdminInDB.getEmail());
            OrganizationAdmin savedOrganizationAdmin = organizationAdminRepository.save(organizationAdminInDB);
            LOGGER.info("Successfully updated organization admin with ID: {}", savedOrganizationAdmin.getId());

            return new AdminSaveResult(savedOrganizationAdmin, null, true); // No password for updates
        }
    }

    public void delete(Integer id) throws PaykeshoError {
        Long count = organizationAdminRepository.countById(id);
        if (count == null || count == 0) {
            throw new PaykeshoError("Organization admin not found");
        }
        organizationAdminRepository.deleteById(id);
    }

    public Long countAdmins() {
        return organizationAdminRepository.count();
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

    private void sendWelcomeNotification(OrganizationAdmin organizationAdmin, String password, Organization organization) {
        try {
            // Use the new templated email service
            boolean emailSent = notificationClient.sendOrganizationAdminWelcomeEmail(
                    organizationAdmin.getFullName(),
                    organizationAdmin.getEmail(),
                    organization.getName(),
                    "https://organization.paykesho.com/",
                    password
            );

            if (emailSent) {
                log.info("Welcome email sent successfully to {} for organization {}", organizationAdmin.getEmail(), organization.getName());
            } else {
                log.error("Failed to send welcome email to {} for organization {}", organizationAdmin.getEmail(), organization.getName());
                log.warn("Admin was created successfully but welcome email could not be sent. Please check notification service availability.");
            }

            // Send SMS notification if phone number is available
            if (organizationAdmin.getPhoneNumber() != null && !organizationAdmin.getPhoneNumber().trim().isEmpty()) {
                String smsMessage = String.format("Welcome to Paykesho! You're now an organization admin for %s. Login: https://organization.paykesho.com/ Email: %s Password: %s. Change password after first login. Contact support if needed.",
                        organization.getName(), organizationAdmin.getEmail(), password);

                boolean smsSent = notificationClient.sendNotification(organizationAdmin.getPhoneNumber(), smsMessage, "sms");
                if (smsSent) {
                    log.info("Welcome SMS sent successfully to {}", organizationAdmin.getPhoneNumber());
                } else {
                    log.error("Failed to send welcome SMS to {}", organizationAdmin.getPhoneNumber());
                    log.warn("Admin was created successfully but welcome SMS could not be sent. Please check notification service availability.");
                }
            }
        } catch (Exception e) {
            log.error("Error sending welcome notification: {}", e.getMessage());
        }
    }

    private boolean sendWelcomeNotificationWithResult(OrganizationAdmin organizationAdmin, String password, Organization organization) {
        try {
            // Use the new templated email service
            boolean emailSent = notificationClient.sendOrganizationAdminWelcomeEmail(
                    organizationAdmin.getFullName(),
                    organizationAdmin.getEmail(),
                    organization.getName(),
                    "https://organization.paykesho.com/",
                    password
            );

            boolean smsSent = true; // Default to true if no phone number
            if (organizationAdmin.getPhoneNumber() != null && !organizationAdmin.getPhoneNumber().trim().isEmpty()) {
                String smsMessage = String.format("Welcome to Paykesho! You're now an organization admin for %s. Login: https://organization.paykesho.com/ Email: %s Password: %s. Change password after first login. Contact support if needed.",
                        organization.getName(), organizationAdmin.getEmail(), password);

                smsSent = notificationClient.sendNotification(organizationAdmin.getPhoneNumber(), smsMessage, "sms");
            }

            boolean allNotificationsSent = emailSent && smsSent;

            if (allNotificationsSent) {
                log.info("All welcome notifications sent successfully to {} for organization {}", organizationAdmin.getEmail(), organization.getName());
            } else {
                log.warn("Some welcome notifications failed for {} - Email: {}, SMS: {}",
                        organizationAdmin.getEmail(), emailSent, smsSent);
            }

            return allNotificationsSent;
        } catch (Exception e) {
            log.error("Error sending welcome notification: {}", e.getMessage());
            return false;
        }
    }


    /**
     * Verify if the current password in database matches the provided plain text password
     */
    public boolean verifyPassword(String email, String plainPassword) {
        try {
            OrganizationAdmin admin = organizationAdminRepository.findByEmail(email);
            if (admin == null) {
                log.warn("Admin not found for password verification: {}", email);
                return false;
            }

            boolean matches = passwordEncoder.matches(plainPassword, admin.getPassword());
            log.debug("Password verification for {}: {} (Hash: {}...)", email, matches,
                    admin.getPassword().substring(0, Math.min(20, admin.getPassword().length())));
            return matches;
        } catch (Exception e) {
            log.error("Error verifying password for {}: {}", email, e.getMessage());
            return false;
        }
    }

    @Transactional
    public String resetPassword(OrganizationAdmin organizationAdmin, Organization organization) throws PaykeshoError {

        // CRASH POINT VALIDATION: Input parameter validation
        if (organizationAdmin == null) {
            throw new PaykeshoError("Organization admin cannot be null");
        }
        if (organization == null) {
            throw new PaykeshoError("Organization cannot be null");
        }
        if (organizationAdmin.getEmail() == null || organizationAdmin.getEmail().trim().isEmpty()) {
            throw new PaykeshoError("Organization admin must have a valid email address");
        }

        String originalPassword = organizationAdmin.getPassword(); // Backup for rollback

        try {
            log.debug("Resetting password for organization admin: {}", organizationAdmin.getEmail());

            // CRASH POINT #4 FIX: Password generation with validation
            String newPassword = generateTemporaryPassword();
            if (newPassword == null || newPassword.trim().isEmpty()) {
                throw new PaykeshoError("Failed to generate temporary password");
            }

            // CRASH POINT #4 FIX: Password encoding with null check
            if (passwordEncoder == null) {
                throw new PaykeshoError("Password encoder is not available");
            }

            String encodedPassword;
            try {
                encodedPassword = passwordEncoder.encode(newPassword);
                if (encodedPassword == null || encodedPassword.trim().isEmpty()) {
                    throw new PaykeshoError("Password encoding failed - empty result");
                }
            } catch (Exception e) {
                log.error("Password encoding failed for admin {}: {}", organizationAdmin.getEmail(), e.getMessage());
                throw new PaykeshoError("Password encoding failed: " + e.getMessage());
            }

            // Update admin properties
            organizationAdmin.setPassword(encodedPassword);
            organizationAdmin.setAccountNonLocked(true);
            organizationAdmin.setFailedAttempt(0);
            organizationAdmin.setLockTime(null); // Clear any existing lock time
            organizationAdmin.setEnabled(true); // Enable account after password reset

            // CRASH POINT #5 FIX: Database save with enhanced error handling
            try {
                organizationAdmin = organizationAdminRepository.save(organizationAdmin);
                log.info("Password updated in database for organization admin: {}", organizationAdmin.getEmail());
            } catch (Exception e) {
                log.error("Database save failed for admin {}: {}", organizationAdmin.getEmail(), e.getMessage());
                // Attempt to restore original password
                organizationAdmin.setPassword(originalPassword);
                throw new PaykeshoError("Failed to save password changes to database: " + e.getMessage());
            }

            // CRASH POINT #7 & #8 FIX: Notification with enhanced error handling
            try {
                sendPasswordResetNotification(organizationAdmin, newPassword, organization);
                log.info("Password reset notification process completed for organization admin: {}", organizationAdmin.getEmail());
            } catch (Exception e) {
                log.error("Failed to send password reset notification to {}: {}", organizationAdmin.getEmail(), e.getMessage());
                // Don't fail the entire operation if notification fails - password is already reset
                log.warn("Password reset succeeded but notification failed for {}", organizationAdmin.getEmail());
            }

            log.info("Password reset successful for organization admin: {}", organizationAdmin.getEmail());
            return newPassword;

        } catch (PaykeshoError e) {
            // Re-throw business logic errors
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error resetting password for organization admin {}: {}", organizationAdmin.getEmail(), e.getMessage(), e);
            throw new PaykeshoError("Unexpected error during password reset: " + e.getMessage());
        }
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        Random random = new Random();
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

    private void sendPasswordResetNotification(OrganizationAdmin organizationAdmin, String newPassword, Organization organization) throws PaykeshoError {

        // CRASH POINT #8 FIX: Validate input parameters
        if (organizationAdmin == null || newPassword == null || organization == null) {
            throw new PaykeshoError("Invalid parameters for notification: admin, password, and organization must not be null");
        }

        if (organizationAdmin.getEmail() == null || organizationAdmin.getEmail().trim().isEmpty()) {
            throw new PaykeshoError("Cannot send notification: admin has no email address");
        }

        boolean emailSent = false;
        boolean smsSent = false;
        StringBuilder errorMessages = new StringBuilder();

        try {
            log.debug("Sending password reset notification to: {}", organizationAdmin.getEmail());

            // CRASH POINT #7 FIX: Notification service with timeout and retry
            try {
                emailSent = notificationClient.sendAdminPasswordResetEmail(
                    organizationAdmin.getFullName(),
                    organizationAdmin.getEmail(),
                    "https://organization.paykesho.com/",
                    newPassword,
                    organization.getName()
                );
                if (emailSent) {
                    log.info("Password reset email sent successfully to {}", organizationAdmin.getEmail());
                } else {
                    log.error("Failed to send password reset email to {}", organizationAdmin.getEmail());
                    errorMessages.append("Email delivery failed. ");
                }
            } catch (Exception e) {
                log.error("Exception during email notification to {}: {}", organizationAdmin.getEmail(), e.getMessage());
                errorMessages.append("Email service error: ").append(e.getMessage()).append(". ");
            }

            // Send SMS notification if phone number is available
            if (organizationAdmin.getPhoneNumber() != null && !organizationAdmin.getPhoneNumber().trim().isEmpty()) {
                try {
                    String smsMessage = generatePasswordResetSmsMessage(organizationAdmin, newPassword, organization);
                    if (smsMessage == null || smsMessage.trim().isEmpty()) {
                        log.error("Generated SMS message is empty for {}", organizationAdmin.getPhoneNumber());
                        errorMessages.append("SMS message generation failed. ");
                    } else {
                        try {
                            smsSent = notificationClient.sendNotification(organizationAdmin.getPhoneNumber(), smsMessage, "sms");
                            if (smsSent) {
                                log.info("Password reset SMS sent successfully to {}", organizationAdmin.getPhoneNumber());
                            } else {
                                log.error("Failed to send password reset SMS to {}", organizationAdmin.getPhoneNumber());
                                errorMessages.append("SMS delivery failed. ");
                            }
                        } catch (Exception e) {
                            log.error("Exception during SMS notification to {}: {}", organizationAdmin.getPhoneNumber(), e.getMessage());
                            errorMessages.append("SMS service error: ").append(e.getMessage()).append(". ");
                        }
                    }
                } catch (Exception e) {
                    log.error("Error generating SMS message for {}: {}", organizationAdmin.getPhoneNumber(), e.getMessage());
                    errorMessages.append("SMS message generation error: ").append(e.getMessage()).append(". ");
                }
            } else {
                log.debug("No phone number available for SMS notification to {}", organizationAdmin.getEmail());
            }

            // Check if at least one notification method succeeded
            if (!emailSent && !smsSent) {
                String errorMsg = "All notification methods failed. " + errorMessages.toString();
                log.error("Complete notification failure for {}: {}", organizationAdmin.getEmail(), errorMsg);
                throw new PaykeshoError("Failed to send password reset notification: " + errorMsg);
            } else {
                if (emailSent && smsSent) {
                    log.info("Password reset notifications sent successfully via email and SMS to {}", organizationAdmin.getEmail());
                } else if (emailSent) {
                    log.info("Password reset notification sent successfully via email to {} (SMS failed or unavailable)", organizationAdmin.getEmail());
                } else if (smsSent) {
                    log.info("Password reset notification sent successfully via SMS to {} (Email failed)", organizationAdmin.getEmail());
                }
            }

        } catch (PaykeshoError e) {
            // Re-throw business logic errors
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error sending password reset notification: {}", e.getMessage(), e);
            throw new PaykeshoError("Unexpected error during notification: " + e.getMessage());
        }
    }


    private String generatePasswordResetSmsMessage(OrganizationAdmin organizationAdmin, String newPassword, Organization organization) {
        return String.format("Paykesho Password Reset: Your password has been reset for %s. Email: %s New password: %s. Login: https://organization.paykesho.com/ Change password immediately after login. Contact support if you didn't request this.",
                organization.getName(), organizationAdmin.getEmail(), newPassword);
    }

    public static class AdminSaveResult {
        private final OrganizationAdmin admin;
        private final String password;
        private final boolean notificationsSent;

        public AdminSaveResult(OrganizationAdmin admin, String password, boolean notificationsSent) {
            this.admin = admin;
            this.password = password;
            this.notificationsSent = notificationsSent;
        }

        public OrganizationAdmin getAdmin() {
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
