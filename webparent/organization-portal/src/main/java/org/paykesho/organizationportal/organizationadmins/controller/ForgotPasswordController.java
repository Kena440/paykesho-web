package org.paykesho.organizationportal.organizationadmins.controller;

import org.commons.entity.organization.OrganizationAdmin;
import org.commons.utils.PaykeshoError;
import org.paykesho.organizationportal.config.Utility;
import org.paykesho.organizationportal.notification.general.NotificationClient;
import org.paykesho.organizationportal.organizationadmins.service.OrganizationAdminService;
import org.paykesho.organizationportal.security.PasswordResetRateLimiter;
import org.paykesho.organizationportal.security.SecureEmailValidator;
import org.paykesho.organizationportal.security.CaptchaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

@Controller
public class ForgotPasswordController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgotPasswordController.class);
    @Autowired
    private OrganizationAdminService organizationAdminService;

    @Autowired
    private NotificationClient notificationClient;
    
    @Autowired
    private PasswordResetRateLimiter rateLimiter;
    
    @Autowired
    private SecureEmailValidator emailValidator;
    
    @Autowired
    private CaptchaService captchaService;

    @PostMapping(value = "/forgot-password")
    public String forgotPasswordForm(
            Model model,
            HttpServletRequest request) throws PaykeshoError {
        String email = request.getParameter("modalPasswordResetEmail");
        
        // SECURITY: Validate email format first (without revealing if it exists)
        SecureEmailValidator.EmailValidationResult emailValidation = emailValidator.validateEmail(email);
        
        if (!emailValidation.isValid()) {
            LOGGER.warn("Invalid email format provided: {}", emailValidation.getMessage());
            model.addAttribute("forgotPasswordMessage", 
                "If an account with this email exists, a password reset link has been sent to your email address.");
            return "security/login";
        }
        
        String clientIp = getClientIpAddress(request);
        String sessionId = request.getSession().getId();
        
        // SECURITY: Check rate limits
        PasswordResetRateLimiter.RateLimitResult rateLimitResult = rateLimiter.checkRateLimit(clientIp, email);
        
        if (!rateLimitResult.isAllowed()) {
            LOGGER.warn("Rate limit exceeded for IP: {}, Email: {}, Reason: {}", clientIp, email, rateLimitResult.getReason());
            model.addAttribute("forgotPasswordMessage", 
                "Too many password reset requests. Please try again later.");
            return "security/login";
        }
        
        // SECURITY: Check CAPTCHA verification
        String captchaAnswer = request.getParameter("captchaAnswer");
        String captchaChallengeId = request.getParameter("captchaChallengeId");
        
        // Check if CAPTCHA fields are provided
        if (captchaAnswer == null || captchaAnswer.trim().isEmpty() || 
            captchaChallengeId == null || captchaChallengeId.trim().isEmpty()) {
            LOGGER.warn("CAPTCHA fields are empty for session: {}. Please load CAPTCHA before submitting.", sessionId);
            model.addAttribute("forgotPasswordMessage", 
                "Please solve the security challenge before submitting the form.");
            return "security/login";
        }
        
        if (!captchaService.verifyChallenge(sessionId, captchaChallengeId, captchaAnswer)) {
            LOGGER.warn("CAPTCHA verification failed for session: {}", sessionId);
            model.addAttribute("forgotPasswordMessage", 
                "Security challenge verification failed. Please try again.");
            return "security/login";
        }
        
        // SECURITY: Always show same message and timing to prevent email enumeration
        long startTime = System.currentTimeMillis();
        
        try {
            // Always attempt to process the request to maintain consistent timing
            processPasswordResetRequest(email, request);
            
            // SECURITY: Always show the same success message regardless of whether email exists
            model.addAttribute("forgotPasswordMessage", 
                "If an account with this email exists, a password reset link has been sent to your email address.");
            
        } catch (Exception e) {
            LOGGER.error("Error processing password reset request for {}: {}", email, e.getMessage());
            // SECURITY: Still show the same message to prevent enumeration
            model.addAttribute("forgotPasswordMessage", 
                "If an account with this email exists, a password reset link has been sent to your email address.");
        }
        
        // SECURITY: Apply progressive delay based on rate limiting
        long elapsedTime = System.currentTimeMillis() - startTime;
        long requiredDelay = Math.max(1000, rateLimitResult.getDelayMs()); // Minimum 1 second
        
        if (elapsedTime < requiredDelay) {
            try {
                Thread.sleep(requiredDelay - elapsedTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        LOGGER.info("Password reset request processed for {} in {}ms", email, System.currentTimeMillis() - startTime);
        return "security/login";
    }
    
    /**
     * Process password reset request securely without revealing if email exists
     * @param email The email address to process
     * @param request The HTTP request
     * @return true if email was sent, false otherwise (but always return true for security)
     */
    private boolean processPasswordResetRequest(String email, HttpServletRequest request) {
        try {
            // Check if email exists in database
            OrganizationAdmin organizationAdmin = organizationAdminService.getOrganizationAdminByEmail(email);
            
            if (organizationAdmin != null) {
                // Email exists - send reset email
                String token = organizationAdminService.updateOrganizationAdminResetToken(email);
                String verifyURL = Utility.getSiteUrl(request) + "/reset-password?token=" + token;
                
                try {
                    sendTokenEmail(verifyURL, email);
                    LOGGER.info("Password reset email sent to existing organization admin: {}", email);
                    return true;
                } catch (Exception e) {
                    LOGGER.error("Failed to send password reset email to {}: {}", email, e.getMessage());
                    // Don't throw - maintain security by not revealing failure
                }
            } else {
                // Email doesn't exist - but don't reveal this
                LOGGER.info("Password reset requested for non-existent organization admin email: {}", email);
            }
            
            // Always return true to maintain consistent behavior
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Error in password reset processing for {}: {}", email, e.getMessage());
            // Always return true to prevent enumeration
            return true;
        }
    }
    
    /**
     * Get client IP address from request, considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in case of multiple proxies
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Generate CAPTCHA challenge for password reset form
     */
    @GetMapping("/forgot-password/captcha")
    @ResponseBody
    public CaptchaService.CaptchaChallenge generateCaptcha(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        CaptchaService.CaptchaChallenge challenge = captchaService.generateChallenge(sessionId);
        
        if (challenge == null) {
            LOGGER.warn("Failed to generate CAPTCHA challenge for session: {}", sessionId);
            return null;
        }
        
        LOGGER.info("Generated CAPTCHA challenge for session: {}", sessionId);
        return challenge;
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        OrganizationAdmin organizationAdmin = organizationAdminService.getOrganizationAdminByResetToken(token);

        if (organizationAdmin != null) {
            model.addAttribute("token", token);
            model.addAttribute("pageTitle", "Password Reset");

        } else {
            model.addAttribute("message", "Invalid token. Organization admin not found");
            model.addAttribute("pageTitle", "Invalid Token");
            model.addAttribute("emoji", "128529");

            return "register/password_reset_message";

        }
        return "register/reset_password";
    }

    @PostMapping(value = "/reset-password-action")
    public String resetPasswordAction(
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletRequest request) throws PaykeshoError {

        String token = request.getParameter("token");
        String password = request.getParameter("password");

        try {
            organizationAdminService.updatePassword(token, password);

            model.addAttribute("message", "You have successfully updated your password");
            model.addAttribute("pageTitle", "Password reset successful");
            model.addAttribute("emoji", "128540");
            return "register/password_reset_message";

        } catch (PaykeshoError e) {
            model.addAttribute("message", "Invalid token. Organization admin not found");
            model.addAttribute("pageTitle", "Password Reset Failed");
            model.addAttribute("emoji", "128529");
            return "register/password_reset_message";
        }

    }


    private void sendTokenEmail(String verifyURL, String email) throws javax.mail.MessagingException, java.io.UnsupportedEncodingException {
        String toAddress = email;
        
        // Use professional template-based password reset email
        boolean emailNotificationSent = notificationClient.sendPasswordResetEmail("Organization Admin", toAddress, verifyURL);

        LOGGER.info("Attempting to send password reset email to: {} with URL: {}", toAddress, verifyURL);
        if (emailNotificationSent) {
            LOGGER.info("Notification sent successfully for {}", toAddress);
        } else {
            LOGGER.error("Failed to send notification for {}", toAddress);
        }
    }

}
