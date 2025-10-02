package org.paykesho.backoffice.organizationadmin.controller;


import org.commons.entity.organization.Organization;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.organizationadmin.service.OrganizationAdminService;
import org.paykesho.backoffice.organizations.enums.Constants;
import org.paykesho.backoffice.organizations.service.OrganizationService;
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
@RequestMapping("/organization-admin")
public class OrganizationAdminController {
    private static final Logger log = LoggerFactory.getLogger(OrganizationAdminController.class);

    @Autowired
    private OrganizationAdminService organizationAdminService;
    @Autowired
    private OrganizationService organizationService;


    @PostMapping("/save")
    public String saveAdmin(OrganizationAdmin organizationAdmin, HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError, MessagingException, UnsupportedEncodingException {
        Organization organization = null;
        try {
            organization = organizationService.getOrganizationById(Integer.valueOf(request.getParameter("organizationId")));
            if (organization != null) {
                organizationAdmin.setAccountNonLocked(true);
                organizationAdmin.setFailedAttempt(0);

                // Check if this is a new admin or updating existing
                boolean isNewAdmin = organizationAdminService.getByEmail(organizationAdmin.getEmail()) == null;

                OrganizationAdminService.AdminSaveResult result = organizationAdminService.saveWithResult(request, organizationAdmin, organization);
                organization.setContactPerson(result.getAdmin());
                organizationService.save(organization);

                if (isNewAdmin) {
                    String message = "Organization Admin created successfully! Welcome email with login credentials has been sent to " + organizationAdmin.getEmail();

                    // If notifications failed, include the password for manual sharing
                    if (!result.isNotificationsSent() && result.getPassword() != null) {
                        message = "Organization Admin created successfully: " + organizationAdmin.getFullName() +
                                "<br><br><strong>⚠️ Notification Service Unavailable</strong><br>" +
                                "The admin was created but welcome email/SMS could not be sent.<br>" +
                                "<strong>Please share these credentials manually:</strong><br>" +
                                "Email: " + organizationAdmin.getEmail() + "<br>" +
                                "Password: " + result.getPassword() + "<br>" +
                                "Login URL: https://organization.paykesho.com/";
                    }

                    redirectAttributes.addFlashAttribute(Constants.MESSAGE, message);
                } else {
                    redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization Admin updated successfully: " + organizationAdmin.getFullName());
                }
            } else {
                throw new PaykeshoError("Organization not found");
            }
        } catch (Exception e) {
            String errorMessage = "Failed to save organization admin: " + e.getMessage();

            // Check if it's a duplicate email error
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry") && e.getMessage().contains("email")) {
                errorMessage = "An organization admin with email '" + organizationAdmin.getEmail() + "' already exists. Please use a different email address.";
            } else if (e.getCause() != null && e.getCause().getMessage() != null &&
                    e.getCause().getMessage().contains("Duplicate entry") && e.getCause().getMessage().contains("email")) {
                errorMessage = "An organization admin with email '" + organizationAdmin.getEmail() + "' already exists. Please use a different email address.";
            }

            redirectAttributes.addFlashAttribute("error", errorMessage);
        }

        return Constants.REDIRECT_ORGANIZATIONS_VIEW + (organization != null ? organization.getId() : "");
    }

    @PostMapping("/api/check-email-exists")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkEmailExists(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();

        try {
            OrganizationAdmin existingAdmin = organizationAdminService.getByEmail(email);
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

    @PostMapping("/toggle-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleOrganizationAdminStatus(@RequestParam Integer adminId, @RequestParam boolean enabled) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.debug("Toggling organization admin status for ID: {} to: {}", adminId, enabled);
            organizationAdminService.updateCustomerEnabledStatus(adminId, enabled);
            
            OrganizationAdmin admin = organizationAdminService.get(adminId);
            String status = enabled ? "enabled" : "disabled";
            String message = String.format("Organization admin '%s' has been %s successfully.", admin.getFullName(), status);
            
            response.put("success", true);
            response.put("message", message);
            response.put("enabled", enabled);
            
            log.info("Successfully {} organization admin: {} (ID: {})", status, admin.getEmail(), adminId);
            
        } catch (Exception e) {
            log.error("Error toggling organization admin status for ID {}: {}", adminId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to update organization admin status: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam Integer adminId, @RequestParam Integer organizationId,
                                RedirectAttributes redirectAttributes) {

        // CRASH POINT #1 FIX: Validate parameters
        if (adminId == null || organizationId == null) {
            log.error("Invalid parameters: adminId={}, organizationId={}", adminId, organizationId);
            redirectAttributes.addFlashAttribute("error", "Invalid request parameters");
            return organizationId != null ? "redirect:/organizations/view/" + organizationId : "redirect:/organizations";
        }

        try {
            log.debug("Resetting password for organization admin ID: {}", adminId);

            // CRASH POINT #2 FIX: Enhanced admin validation
            OrganizationAdmin admin = organizationAdminService.get(adminId);
            if (admin == null) {
                log.warn("Organization admin not found for ID: {}", adminId);
                redirectAttributes.addFlashAttribute("error", "Organization admin not found");
                return "redirect:/organizations/view/" + organizationId;
            }

            // CRASH POINT #3 FIX: Enhanced organization validation
            Organization organization = organizationService.getOrganizationById(organizationId);
            if (organization == null) {
                log.warn("Organization not found for ID: {}", organizationId);
                redirectAttributes.addFlashAttribute("error", "Organization not found");
                return "redirect:/organizations/view/" + organizationId;
            }

            // Additional validation for admin state
            if (admin.getEmail() == null || admin.getEmail().trim().isEmpty()) {
                log.error("Organization admin {} has no email address", adminId);
                redirectAttributes.addFlashAttribute("error", "Organization admin has no email address configured");
                return "redirect:/organizations/view/" + organizationId;
            }

            // Generate new temporary password and send email
            organizationAdminService.resetPassword(admin, organization);

            // CRASH POINT #6 FIX: Handle thread interruption gracefully
            try {
                Thread.sleep(1000); // 1 second delay
            } catch (InterruptedException ie) {
                log.warn("Thread sleep interrupted during password reset for admin {}", admin.getEmail());
                Thread.currentThread().interrupt(); // Restore interrupted status
                // Continue processing - interruption is not critical here
            }

            log.info("Password reset successful for organization admin: {} ({})", admin.getFullName(), admin.getEmail());
            redirectAttributes.addFlashAttribute(Constants.MESSAGE,
                    "Password reset successful! A new temporary password has been sent to " + admin.getEmail() +
                            ". Please wait a moment before attempting to login.");

        } catch (IllegalArgumentException e) {
            log.error("Invalid argument during password reset for admin ID {}: {}", adminId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Invalid request: " + e.getMessage());
        } catch (PaykeshoError e) {
            log.error("Business logic error during password reset for admin ID {}: {}", adminId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Password reset failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error resetting password for organization admin ID {}: {}", adminId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred. Please try again or contact support.");
        }

        return "redirect:/organizations/view/" + organizationId;
    }

}
