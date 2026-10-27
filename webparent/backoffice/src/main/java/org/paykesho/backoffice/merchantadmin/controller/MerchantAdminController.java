package org.paykesho.backoffice.merchantadmin.controller;


import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.MerchantAdmin;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.merchantadmin.service.MerchantAdminService;
import org.paykesho.backoffice.merchants.enums.Constants;
import org.paykesho.backoffice.merchants.service.MerchantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/merchant-admin")
public class MerchantAdminController {
    private static final Logger log = LoggerFactory.getLogger(MerchantAdminController.class);

    @Autowired
    private MerchantAdminService memberAdminService;
    @Autowired
    private MerchantService merchantService;


    @PostMapping("/save")
    public String saveAdmin(MerchantAdmin merchantAdmin, HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError, MessagingException, UnsupportedEncodingException {
        Merchant merchant = null;
        try {
            merchant = merchantService.getMerchantById(Integer.valueOf(request.getParameter("merchantId")));
            if (merchant != null) {
                merchantAdmin.setAccountNonLocked(true);
                merchantAdmin.setFailedAttempt(0);

                // Check if this is a new admin or updating existing
                boolean isNewAdmin = memberAdminService.getByEmail(merchantAdmin.getEmail()) == null;

                MerchantAdminService.AdminSaveResult result = memberAdminService.saveWithResult(request, merchantAdmin, merchant);
                merchant.setContactPerson(result.getAdmin());
                merchantService.save(merchant);

                if (isNewAdmin) {
                    String message = "Merchant Admin created successfully! Welcome email with login credentials has been sent to " + merchantAdmin.getEmail();

                    // If notifications failed, include the password for manual sharing
                    if (!result.isNotificationsSent() && result.getPassword() != null) {
                        message = "Merchant Admin created successfully: " + org.springframework.web.util.HtmlUtils.htmlEscape(String.valueOf(merchantAdmin.getFullName())) +
                                "<br><br><strong>Share these sign-in details with the new admin</strong><br>" +
                                "Welcome emails and SMS are not being sent yet, so pass these on securely. This is the only time the password is shown.<br>" +
                                "Email: " + org.springframework.web.util.HtmlUtils.htmlEscape(String.valueOf(merchantAdmin.getEmail())) + "<br>" +
                                "Password: <code>" + org.springframework.web.util.HtmlUtils.htmlEscape(result.getPassword()) + "</code><br>" +
                                "Sign-in page: " + org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath().replacePath("/merchant/login").build().toUriString();
                    }

                    redirectAttributes.addFlashAttribute(Constants.MESSAGE, message);
                    redirectAttributes.addFlashAttribute("messageHtml", true); // built above with escaped values
                } else {
                    redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Merchant Admin updated successfully: " + merchantAdmin.getFullName());
                }
            } else {
                throw new PaykeshoError("Merchant not found");
            }
        } catch (Exception e) {
            String errorMessage = "Failed to save merchant admin: " + e.getMessage();

            // Check if it's a duplicate email error
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry") && e.getMessage().contains("email")) {
                errorMessage = "A merchant admin with email '" + merchantAdmin.getEmail() + "' already exists. Please use a different email address.";
            } else if (e.getCause() != null && e.getCause().getMessage() != null &&
                    e.getCause().getMessage().contains("Duplicate entry") && e.getCause().getMessage().contains("email")) {
                errorMessage = "A merchant admin with email '" + merchantAdmin.getEmail() + "' already exists. Please use a different email address.";
            }

            redirectAttributes.addFlashAttribute("error", errorMessage);
        }

        return Constants.REDIRECT_MERCHANTS_VIEW + (merchant != null ? merchant.getUuid() : "");
    }

    @PostMapping("/api/check-email-exists")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkEmailExists(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();

        try {
            MerchantAdmin existingAdmin = memberAdminService.getByEmail(email);
            boolean exists = existingAdmin != null;

            response.put("exists", exists);
            response.put("success", true);

            if (exists && existingAdmin != null) {
                response.put("message", "Email already exists in the system");
                response.put("adminName", existingAdmin.getFullName());
            } else {
                response.put("message", "Email is available");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("exists", false);
            response.put("success", false);
            response.put("message", "Error checking email: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String adminUuid, @RequestParam String merchantUuid,
                                RedirectAttributes redirectAttributes) {
        try {
            log.debug("Resetting password for merchant admin UUID: {}", adminUuid);

            MerchantAdmin admin = memberAdminService.getByUuid(adminUuid);
            if (admin == null) {
                redirectAttributes.addFlashAttribute("error", "Merchant admin not found");
                return "redirect:/merchants/view/" + merchantUuid;
            }

            Merchant merchant = merchantService.getMerchantByUuid(merchantUuid);
            if (merchant == null) {
                redirectAttributes.addFlashAttribute("error", "Merchant not found");
                return "redirect:/merchants/view/" + merchantUuid;
            }

            // Generate new temporary password and send email
            String newPassword = memberAdminService.resetPassword(admin, merchant);

            log.info("Password reset successful for merchant admin: {} ({})", admin.getFullName(), admin.getEmail());
            if (memberAdminService.isNotificationDeliveryEnabled()) {
                redirectAttributes.addFlashAttribute(Constants.MESSAGE,
                        "Password reset successful! A new temporary password has been sent to " + admin.getEmail());
            } else {
                redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Password reset for " + org.springframework.web.util.HtmlUtils.htmlEscape(String.valueOf(admin.getEmail())) + ". Share the new password securely (this is the only time it is shown): <code>" + org.springframework.web.util.HtmlUtils.htmlEscape(newPassword) + "</code><br>Sign-in page: " + org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath().replacePath("/merchant/login").build().toUriString());
                redirectAttributes.addFlashAttribute("messageHtml", true);
            }

        } catch (Exception e) {
            log.error("Error resetting password for merchant admin UUID {}: {}", adminUuid, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to reset password: " + e.getMessage());
        }

        return "redirect:/merchants/view/" + merchantUuid;
    }

}
