package org.paykesho.notificationservice.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    /**
     * Generate welcome email for new admin users
     */
    public String generateWelcomeAdminEmail(String adminName, String adminEmail, String portalUrl, String temporaryPassword) {
        Context context = new Context();
        context.setVariable("subject", "Welcome to Paykesho - Admin Account Created");
        context.setVariable("greeting", "Hello " + adminName + ",");
        context.setVariable("recipientEmail", adminEmail);
        context.setVariable("portalUrl", portalUrl);
        context.setVariable("adminEmail", adminEmail);
        context.setVariable("temporaryPassword", temporaryPassword);

        return templateEngine.process("email/welcome-admin", context);
    }

    /**
     * Generate password reset email
     */
    public String generatePasswordResetEmail(String userName, String userEmail, String resetUrl) {
        Context context = new Context();
        context.setVariable("subject", "Password Reset Request - Paykesho");
        context.setVariable("greeting", "Hello " + userName + ",");
        context.setVariable("recipientEmail", userEmail);
        context.setVariable("resetUrl", resetUrl);

        return templateEngine.process("email/password-reset", context);
    }

    /**
     * Generate account locked notification email
     */
    public String generateAccountLockedEmail(String userName, String userEmail, LocalDateTime lockTime, LocalDateTime unlockTime) {
        Context context = new Context();
        context.setVariable("subject", "Account Security Alert - Paykesho");
        context.setVariable("greeting", "Hello " + userName + ",");
        context.setVariable("recipientEmail", userEmail);
        context.setVariable("userEmail", userEmail);
        context.setVariable("lockTime", lockTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        context.setVariable("unlockTime", unlockTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return templateEngine.process("email/account-locked", context);
    }

    /**
     * Generate custom email with dynamic content
     */
    public String generateCustomEmail(String subject, String greeting, String content, String recipientEmail,
                                      String highlightTitle, String highlightContent,
                                      String warningTitle, String warningContent,
                                      String actionUrl, String actionText) {
        Context context = new Context();
        context.setVariable("subject", subject);
        context.setVariable("greeting", greeting);
        context.setVariable("content", content);
        context.setVariable("recipientEmail", recipientEmail);
        context.setVariable("highlightTitle", highlightTitle);
        context.setVariable("highlightContent", highlightContent);
        context.setVariable("warningTitle", warningTitle);
        context.setVariable("warningContent", warningContent);
        context.setVariable("actionUrl", actionUrl);
        context.setVariable("actionText", actionText);

        return templateEngine.process("email/base", context);
    }

    /**
     * Generate simple notification email
     */
    public String generateSimpleEmail(String subject, String greeting, String content, String recipientEmail) {
        return generateCustomEmail(subject, greeting, content, recipientEmail, null, null, null, null, null, null);
    }

    /**
     * Generate merchant admin welcome email
     */
    public String generateMerchantAdminWelcomeEmail(String adminName, String adminEmail, String merchantName, String portalUrl, String temporaryPassword) {
        try {
            log.info("Generating merchant admin welcome email template for: {} ({})", adminName, adminEmail);
            log.info("Template variables - adminName: {}, adminEmail: {}, merchantName: {}, portalUrl: {}, temporaryPassword: {}",
                    adminName, adminEmail, merchantName, portalUrl, temporaryPassword);

            Context context = new Context();
            context.setVariable("subject", "Welcome to Paykesho Merchant Portal");
            context.setVariable("greeting", "Hello " + adminName + ",");
            context.setVariable("recipientEmail", adminEmail);
            context.setVariable("portalUrl", portalUrl);
            context.setVariable("adminEmail", adminEmail);
            context.setVariable("temporaryPassword", temporaryPassword);
            context.setVariable("merchantName", merchantName);

            // Custom content for merchant admin
            String content = "<p>Welcome to <strong>Paykesho</strong>! We're excited to have you on board as a merchant administrator.</p>" +
                    "<p>Your account has been successfully created and you now have access to manage <strong>" + merchantName + "</strong> on our merchant portal.</p>";

            context.setVariable("content", content);

            log.info("Context variables set - subject: {}, greeting: {}, content: {}",
                    context.getVariable("subject"), context.getVariable("greeting"), context.getVariable("content"));

            String result = templateEngine.process("email/welcome-admin", context);
            log.info("Successfully generated merchant admin welcome email template, length: {}", result != null ? result.length() : 0);
            log.info("Template result preview (first 500 chars): {}", result != null ? result.substring(0, Math.min(500, result.length())) : "null");
            return result;
        } catch (Exception e) {
            log.error("Error generating merchant admin welcome email template: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Generate organization admin welcome email
     */
    public String generateOrganizationAdminWelcomeEmail(String adminName, String adminEmail, String organizationName, String portalUrl, String temporaryPassword) {
        try {
            log.info("Generating organization admin welcome email template for: {} ({})", adminName, adminEmail);
            log.info("Template variables - adminName: {}, adminEmail: {}, organizationName: {}, portalUrl: {}, temporaryPassword: {}",
                    adminName, adminEmail, organizationName, portalUrl, temporaryPassword);

            Context context = new Context();
            context.setVariable("subject", "Welcome to Paykesho Organization Portal");
            context.setVariable("greeting", "Hello " + adminName + ",");
            context.setVariable("recipientEmail", adminEmail);
            context.setVariable("portalUrl", portalUrl);
            context.setVariable("adminEmail", adminEmail);
            context.setVariable("temporaryPassword", temporaryPassword);
            context.setVariable("organizationName", organizationName);

            // Custom content for organization admin
            String content = "<p>Welcome to <strong>Paykesho</strong>! We're excited to have you on board as an organization administrator.</p>" +
                    "<p>Your account has been successfully created and you now have access to manage <strong>" + organizationName + "</strong> on our organization portal.</p>";

            context.setVariable("content", content);

            log.info("Context variables set - subject: {}, greeting: {}, content: {}",
                    context.getVariable("subject"), context.getVariable("greeting"), context.getVariable("content"));

            String result = templateEngine.process("email/welcome-admin", context);
            log.info("Successfully generated organization admin welcome email template, length: {}", result != null ? result.length() : 0);
            log.info("Template result preview (first 500 chars): {}", result != null ? result.substring(0, Math.min(500, result.length())) : "null");
            return result;
        } catch (Exception e) {
            log.error("Error generating organization admin welcome email template: {}", e.getMessage(), e);
            throw e;
        }
    }
}
