package org.paykesho.notificationservice.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    /**
     * Convert image file to base64 data URI
     */
    private String getLogoAsBase64() {
        try {
            ClassPathResource resource = new ClassPathResource("static/images/paykeshologo.png");
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] imageBytes = inputStream.readAllBytes();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                return "data:image/png;base64," + base64Image;
            }
        } catch (IOException e) {
            log.error("Failed to load logo image for email template", e);
            return ""; // Return empty string if image can't be loaded
        }
    }

    /**
     * Generate welcome email for new admin users
     */
    public String generateWelcomeAdminEmail(String adminName, String adminEmail, String portalUrl, String temporaryPassword) {
        if (adminName == null || adminEmail == null || portalUrl == null || temporaryPassword == null) {
            log.error("Null parameters detected in generateWelcomeAdminEmail: adminName={}, adminEmail={}, portalUrl={}, temporaryPassword={}", 
                     adminName, adminEmail, portalUrl, temporaryPassword != null ? "[PROVIDED]" : null);
            throw new IllegalArgumentException("All parameters must be non-null for welcome admin email generation");
        }
        
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
        if (userName == null || userEmail == null || resetUrl == null) {
            log.error("Null parameters detected in generatePasswordResetEmail: userName={}, userEmail={}, resetUrl={}", 
                     userName, userEmail, resetUrl);
            throw new IllegalArgumentException("All parameters must be non-null for password reset email generation");
        }
        
        log.info("Generating password reset email for user: {} ({}) with reset URL: {}", userName, userEmail, resetUrl);
        
        Context context = new Context();
        context.setVariable("subject", "Password Reset Request - Paykesho");
        context.setVariable("greeting", "Hello " + userName + ",");
        context.setVariable("recipientEmail", userEmail);
        
        // Main content for the email
        String content = "<p>We received a request to reset your password for your Paykesho account.</p>" +
                        "<p>If you didn't make this request, you can safely ignore this email. Your password will remain unchanged.</p>";
        
        // Highlight box content with reset button
        String highlightContent = "<h3>Reset Your Password</h3>" +
                                 "<p>Click the button below to reset your password. This link will expire in 24 hours for security reasons.</p>" +
                                 "<div style=\"text-align: center; margin: 20px 0;\">" +
                                 "<a href=\"" + resetUrl + "\" style=\"display: inline-block; background: linear-gradient(135deg, #007bff 0%, #0056b3 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 25px; font-weight: bold;\">Reset Password</a>" +
                                 "</div>";
        
        // Warning content
        String warningContent = "<h4>Security Notice</h4>" +
                               "<ul>" +
                               "<li>This link will expire in 24 hours</li>" +
                               "<li>Never share this link with anyone</li>" +
                               "<li>If you didn't request this reset, please contact support immediately</li>" +
                               "<li>Use a strong, unique password when setting your new password</li>" +
                               "</ul>" +
                               "<p>If the button above doesn't work, you can copy and paste this link into your browser:</p>" +
                               "<p style=\"word-break: break-all; background-color: #f8f9fa; padding: 10px; border-radius: 5px; font-family: monospace; font-size: 12px;\">" + resetUrl + "</p>" +
                               "<p>Best regards,<br><strong>The Paykesho Security Team</strong></p>";
        
        context.setVariable("content", content);
        context.setVariable("highlightContent", highlightContent);
        context.setVariable("warningContent", warningContent);
        // Set logo as base64 data URI for embedded image
        context.setVariable("logoUrl", getLogoAsBase64());
        log.info("Password reset email context variables set - content length: {}, highlight length: {}, warning length: {}", 
                content.length(), highlightContent.length(), warningContent.length());

        String result = templateEngine.process("email/base", context);
        log.info("Successfully generated password reset email template, length: {}", result != null ? result.length() : 0);
        
        return result;
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
     * Generate voucher notification email
     */
    public String generateVoucherNotificationEmail(String clientName, String clientEmail, String voucherReference, 
                                                   String actionType, String message, BigDecimal amount) {
        if (clientName == null || clientEmail == null || voucherReference == null || actionType == null || amount == null) {
            log.error("Null parameters detected in generateVoucherNotificationEmail: clientName={}, clientEmail={}, voucherReference={}, actionType={}, amount={}", 
                     clientName, clientEmail, voucherReference, actionType, amount);
            throw new IllegalArgumentException("All required parameters must be non-null for voucher notification email generation");
        }
        
        Context context = new Context();
        context.setVariable("subject", "Voucher Update - Paykesho");
        context.setVariable("greeting", "Hello " + clientName + ",");
        context.setVariable("recipientEmail", clientEmail);
        
        String content = "<p>We have an update regarding your voucher request.</p>";
        
        String highlightContent = "";
        String warningContent = "";
        
        switch (actionType) {
            case "REQUEST":
                content = "<p>Your voucher request has been submitted successfully.</p>";
                highlightContent = "<p><strong>Voucher Reference:</strong> " + voucherReference + "</p>" +
                                 "<p>Please allow some time for processing.</p>";
                break;
            case "APPROVAL":
                content = "<p>Great news! Your voucher request has been approved.</p>";
                highlightContent = "<p><strong>Voucher Reference:</strong> " + voucherReference + "</p>" +
                                 "<p><strong>Approved Amount:</strong> K" + amount + "</p>" +
                                 "<p>You will receive further instructions shortly.</p>";
                break;
            case "REJECTION":
                content = "<p>Your voucher request has been reviewed.</p>";
                warningContent = "<p><strong>Status:</strong> Rejected</p>" +
                               "<p><strong>Reason:</strong> " + message + "</p>" +
                               "<p>Please contact our support team if you have any questions.</p>";
                break;
            case "ADJUSTMENT":
                content = "<p>Your voucher request has been adjusted.</p>";
                highlightContent = "<p><strong>New Amount:</strong> K" + amount + "</p>" +
                                 "<p><strong>Voucher Reference:</strong> " + voucherReference + "</p>";
                break;
            default:
                content = "<p>We have an update regarding your voucher request.</p>";
                break;
        }
        
        context.setVariable("content", content);
        context.setVariable("highlightContent", highlightContent);
        context.setVariable("warningContent", warningContent);
        
        return templateEngine.process("email/base", context);
    }

    /**
     * Generate transaction notification email
     */
    public String generateTransactionNotificationEmail(String clientName, String clientEmail, String transactionReference, 
                                                       String merchantName, BigDecimal amount) {
        if (clientName == null || clientEmail == null || transactionReference == null || merchantName == null || amount == null) {
            log.error("Null parameters detected in generateTransactionNotificationEmail: clientName={}, clientEmail={}, transactionReference={}, merchantName={}, amount={}", 
                     clientName, clientEmail, transactionReference, merchantName, amount);
            throw new IllegalArgumentException("All required parameters must be non-null for transaction notification email generation");
        }
        
        Context context = new Context();
        context.setVariable("subject", "Payment Confirmation - Paykesho");
        context.setVariable("greeting", "Hello " + clientName + ",");
        context.setVariable("recipientEmail", clientEmail);
        
        String content = "<p>Your payment has been processed successfully.</p>";
        
        String highlightContent = "<p><strong>Transaction Details:</strong></p>" +
                                "<p><strong>Amount:</strong> K" + amount + "</p>" +
                                "<p><strong>Merchant:</strong> " + merchantName + "</p>" +
                                "<p><strong>Reference:</strong> " + transactionReference + "</p>" +
                                "<p>Thank you for using Paykesho for your payment.</p>";
        
        context.setVariable("content", content);
        context.setVariable("highlightContent", highlightContent);
        
        return templateEngine.process("email/base", context);
    }

    /**
     * Generate admin password reset email (for when admin resets user's password)
     */
    public String generateAdminPasswordResetEmail(String adminName, String adminEmail, String portalUrl, 
                                                 String temporaryPassword, String entityName) {
        if (adminName == null || adminEmail == null || portalUrl == null || temporaryPassword == null || entityName == null) {
            log.error("Null parameters detected in generateAdminPasswordResetEmail: adminName={}, adminEmail={}, portalUrl={}, temporaryPassword={}, entityName={}", 
                     adminName, adminEmail, portalUrl, temporaryPassword != null ? "[PROVIDED]" : null, entityName);
            throw new IllegalArgumentException("All required parameters must be non-null for admin password reset email generation");
        }
        
        Context context = new Context();
        context.setVariable("subject", "Password Reset - Paykesho Admin");
        context.setVariable("greeting", "Hello " + adminName + ",");
        context.setVariable("recipientEmail", adminEmail);
        
        String content = "<p>Your password has been reset for your <strong>" + entityName + "</strong> admin account.</p>" +
                        "<p>For security reasons, a new temporary password has been generated for your account.</p>";
        
        String highlightContent = "<p><strong>Portal URL:</strong> <a href=\"" + portalUrl + "\">" + portalUrl + "</a></p>" +
                                "<p><strong>Email:</strong> " + adminEmail + "</p>" +
                                "<p><strong>New Temporary Password:</strong> <code style=\"background-color: #fff; padding: 2px 6px; border-radius: 3px; font-family: monospace;\">" + temporaryPassword + "</code></p>";
        
        String warningContent = "<ul>" +
                              "<li><strong>You must change this password immediately</strong> after your first login</li>" +
                              "<li>This temporary password will expire in 24 hours</li>" +
                              "<li>Use a strong, unique password with at least 8 characters</li>" +
                              "<li>Include uppercase, lowercase, numbers, and special characters</li>" +
                              "<li>Never share your credentials with others</li>" +
                              "<li>If you did not request this password reset, contact support immediately</li>" +
                              "</ul>";
        
        context.setVariable("content", content);
        context.setVariable("highlightContent", highlightContent);
        context.setVariable("warningContent", warningContent);
        
        return templateEngine.process("email/base", context);
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

    /**
     * Generate registration welcome email
     */
    public String generateRegistrationWelcomeEmail(String userName, String userEmail, String userType) {
        Context context = new Context();
        context.setVariable("subject", "Welcome to Paykesho - Account Created");
        context.setVariable("greeting", "Hello " + userName + ",");
        context.setVariable("recipientEmail", userEmail);
        
        String content = "<p>Welcome to <strong>Paykesho</strong>! Your " + userType + " account has been successfully created.</p>" +
                        "<p>Your account is now active and ready to use.</p>";
        
        String highlightContent = "<p><strong>Account Details:</strong></p>" +
                                "<p><strong>Email:</strong> " + userEmail + "</p>" +
                                "<p>Please check your email for verification instructions to complete your account setup.</p>";
        
        context.setVariable("content", content);
        context.setVariable("highlightContent", highlightContent);
        
        return templateEngine.process("email/base", context);
    }

    /**
     * Generate repayment completion email
     */
    public String generateRepaymentCompletionEmail(String clientName, String clientEmail, String repaymentReference, 
                                                   BigDecimal amount, String date) {
        Context context = new Context();
        context.setVariable("subject", "Repayment Confirmation - Paykesho");
        context.setVariable("greeting", "Hello " + clientName + ",");
        context.setVariable("recipientEmail", clientEmail);
        
        String content = "<p>Your credit repayment has been successfully processed by your employer.</p>";
        
        String highlightContent = "<p><strong>Repayment Details:</strong></p>" +
                                "<p><strong>Reference:</strong> " + repaymentReference + "</p>" +
                                "<p><strong>Amount Paid:</strong> K" + amount + "</p>" +
                                "<p><strong>Date:</strong> " + date + "</p>" +
                                "<p>Thank you for using Paykesho for your credit needs.</p>";
        
        context.setVariable("content", content);
        context.setVariable("highlightContent", highlightContent);
        
        return templateEngine.process("email/base", context);
    }

    /**
     * Generate admin repayment notification email
     */
    public String generateAdminRepaymentNotificationEmail(String clientName, String clientEmail, String repaymentReference, 
                                                         BigDecimal amount, String date) {
        Context context = new Context();
        context.setVariable("subject", "Repayment Completion Notification - Paykesho");
        context.setVariable("greeting", "Hello Admin,");
        
        String content = "<p>A credit repayment has been successfully processed.</p>";
        
        String highlightContent = "<p><strong>Repayment Details:</strong></p>" +
                                "<p><strong>Client:</strong> " + clientName + " (" + clientEmail + ")</p>" +
                                "<p><strong>Reference:</strong> " + repaymentReference + "</p>" +
                                "<p><strong>Amount:</strong> K" + amount + "</p>" +
                                "<p><strong>Date:</strong> " + date + "</p>";
        
        context.setVariable("content", content);
        context.setVariable("highlightContent", highlightContent);
        
        return templateEngine.process("email/base", context);
    }
}
