package org.paykesho.organizationportal.organizationadmins.controller;

import org.commons.entity.organization.OrganizationAdmin;
import org.commons.enums.NotificationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.organizationportal.config.Utility;
import org.paykesho.organizationportal.notification.general.NotificationClient;
import org.paykesho.organizationportal.organizationadmins.service.OrganizationAdminService;
import org.paykesho.organizationportal.settings.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.paykesho.organizationportal.settings.email.EmailSettingBag;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.servlet.http.HttpServletRequest;

@Controller
public class ForgotPasswordController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgotPasswordController.class);
    @Autowired
    private OrganizationAdminService organizationAdminService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private NotificationClient notificationClient;

    @PostMapping(value = "/forgot-password")
    public String forgotPasswordForm(
            Model model,
            HttpServletRequest request) throws PaykeshoError {
        String email = request.getParameter("modalPasswordResetEmail");
        try {
            String token = organizationAdminService.updateOrganizationAdminResetToken(email);
            String verifyURL = Utility.getSiteUrl(request) + "/reset-password?token=" + token;

            new Thread(() -> {
                try {
                    sendTokenEmail(verifyURL, email);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }).start();

            model.addAttribute("forgotPasswordMessage", "Reset link has been sent. Kindly check email");

        } catch (PaykeshoError e) {
            model.addAttribute("forgotPasswordMessage", e.getMessage());
        }

        return "security/login";
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
        String subject = "Password Reset";
        String content = "<p>Hello,</p>"
                + "<p>You requested a password reset. Click the link below to change your password:</p>"
                + "<p><a href=\"" + verifyURL + "\">Change Password</a></p>"
                + "<p>If you did not request this, please ignore the email.</p>";

        boolean emailNotificationSent = notificationClient.sendNotification(toAddress, content, NotificationType.email.defaultDescription());
        if (emailNotificationSent) {
            LOGGER.info("Notification sent successfully for {}", toAddress);
        } else {
            LOGGER.error("Failed to send notification for {}", toAddress);
        }
    }

}
