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

            String encodedPassword = passwordEncoder.encode(securePassword);
            organizationAdminInForm.setPassword(encodedPassword);

            organizationAdminInForm.setVerificationCode(verificationCode);
            organizationAdminInForm.setCreatedTime(new Date());
            organizationAdminInForm.setEnabled(true);
            organizationAdminInForm.setAuthenticationType(AuthenticationType.DATABASE);
            organizationAdminInForm.setResetPasswordToken(token);
            organizationAdminInForm.setFailedAttempt(0);
            organizationAdminInForm.setAccountNonLocked(true);

            // Send notifications asynchronously
            new Thread(() -> {
                try {
                    sendWelcomeNotification(organizationAdminInForm, securePassword, organization);
                } catch (Exception e) {
                    LOGGER.error("Failed to send welcome notification: {}", e.getMessage());
                }
            }).start();

        } else {
            // Update existing admin - preserve existing credentials
            organizationAdminInForm.setResetPasswordToken(organizationAdminInDB.getResetPasswordToken());
            organizationAdminInForm.setAuthenticationType(organizationAdminInDB.getAuthenticationType());
            organizationAdminInForm.setPassword(organizationAdminInDB.getPassword());
            organizationAdminInForm.setVerificationCode(organizationAdminInDB.getVerificationCode());
            organizationAdminInForm.setCreatedTime(organizationAdminInDB.getCreatedTime());
            organizationAdminInForm.setEnabled(organizationAdminInDB.isEnabled());
            organizationAdminInForm.setFailedAttempt(organizationAdminInDB.getFailedAttempt());
            organizationAdminInForm.setAccountNonLocked(organizationAdminInDB.isAccountNonLocked());
        }

        return organizationAdminRepository.save(organizationAdminInForm);
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
                }
            }
        } catch (Exception e) {
            log.error("Error sending welcome notification: {}", e.getMessage());
        }
    }

    private String generateWelcomeEmailMessage(OrganizationAdmin organizationAdmin, String password, Organization organization) {
        StringBuilder message = new StringBuilder();
        message.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        message.append("<div style='background-color: #f8f9fa; padding: 20px; border-radius: 8px;'>");
        message.append("<h2 style='color: #007bff; margin-bottom: 20px;'>Welcome to Paykesho Organization Portal!</h2>");
        message.append("<p>Hello <strong>").append(organizationAdmin.getFullName()).append("</strong>,</p>");
        message.append("<p>You have been successfully registered as an organization admin for <strong>").append(organization.getName()).append("</strong>.</p>");
        message.append("<div style='background-color: #e9ecef; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
        message.append("<h4 style='margin-top: 0; color: #495057;'>Your Login Credentials:</h4>");
        message.append("<p><strong>Login URL:</strong> <a href='https://organization.paykesho.com/' style='color: #007bff;'>https://organization.paykesho.com/</a></p>");
        message.append("<p><strong>Email:</strong> ").append(organizationAdmin.getEmail()).append("</p>");
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

    private String generateWelcomeSmsMessage(OrganizationAdmin organizationAdmin, String password, Organization organization) {
        return String.format("Welcome to Paykesho! You're now an organization admin for %s. Login: https://organization.paykesho.com/ Email: %s Password: %s. Change password after first login. Contact support if needed.", 
            organization.getName(), organizationAdmin.getEmail(), password);
    }
}
