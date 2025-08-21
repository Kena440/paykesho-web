package org.paykesho.backoffice.organizationadmin.controller;


import org.commons.entity.organization.Organization;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.organizationadmin.service.OrganizationAdminService;
import org.paykesho.backoffice.organizations.enums.Constants;
import org.paykesho.backoffice.organizations.service.OrganizationService;
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
                
                OrganizationAdmin admin = organizationAdminService.save(request, organizationAdmin, organization);
                organization.setContactPerson(admin);
                organizationService.save(organization);
                
                if (isNewAdmin) {
                    redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization Admin created successfully! Welcome email with login credentials has been sent to " + organizationAdmin.getEmail());
                } else {
                    redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization Admin updated successfully: " + organizationAdmin.getFullName());
                }
            } else {
                throw new PaykeshoError("Organization not found");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save organization admin: " + e.getMessage());
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
            
            if (exists) {
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

}
