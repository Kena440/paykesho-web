package org.paykesho.clientportal.clients.controller;

import org.commons.entity.client.Client;
import org.commons.enums.NotificationType;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.config.Utility;
import org.paykesho.clientportal.notifications.service.NotificationService;
import org.paykesho.clientportal.settings.email.EmailSettingBag;
import org.paykesho.clientportal.settings.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

@Controller
public class ForgotPasswordController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgotPasswordController.class);
    @Autowired
    private ClientService clientService;
    @Autowired
    private SettingService settingService;
    @Autowired
    NotificationService notificationService;

    @PostMapping(value = "/forgot-password")
    public String forgotPasswordForm(Model model, HttpServletRequest request) throws PaykeshoError {
        String email = request.getParameter("modalPasswordResetEmail");
        try {
            String token = clientService.updateClientResetToken(email);
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
        Client client = clientService.getClientByResetToken(token);

        if (client != null) {
            model.addAttribute("token", token);
            model.addAttribute("pageTitle", "Password Reset");

        } else {
            model.addAttribute("message", "invalid token. Member Admin not found");
            model.addAttribute("pageTitle", "Invalid Token");
            model.addAttribute("emoji", "128529");

            return "register/password_reset_message";

        }
        return "register/reset_password";
    }

    @PostMapping(value = "/reset-password-action")
    public String resetPasswordAction(RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) throws PaykeshoError {

        String token = request.getParameter("token");
        String password = request.getParameter("password");

        try {
            clientService.updatePassword(token, password);

            model.addAttribute("message", "You have successfully updated your password");
            model.addAttribute("pageTitle", "Password reset successful");
            model.addAttribute("emoji", "128540");
            return "register/password_reset_message";

        } catch (PaykeshoError e) {
            model.addAttribute("message", "Invalid token. Member Admin not found");
            model.addAttribute("pageTitle", "Password Reset Failed");
            model.addAttribute("emoji", "128529");
            return "register/password_reset_message";
        }

    }


    private void sendTokenEmail(String verifyURL, String email) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = email;
        String subject = "Password Reset";
        String content = "<p>Hello,</p><p>You requested a password reset. Click the link below to change your password:</p>" + "<p><a href=\"" + verifyURL + "\">Change Password</a></p>" + "<p>If you did not request this, please ignore the email.</p>";

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBags.getFromAddress(), emailSettingBags.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        helper.setText(content, true);

        ////mailSender.send(mimeMessage);

        boolean emailNotificationSent = notificationService.sendNotification(toAddress, content, NotificationType.email.defaultDescription());

        if (emailNotificationSent) {
            System.out.println("Notification sent successfully for " + toAddress);
        } else {
            System.err.println("Failed to send notification for " + toAddress);
        }
    }

}
