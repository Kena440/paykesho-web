package org.paykesho.backoffice.merchantadmin.controller;


import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.MerchantAdmin;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.merchantadmin.service.MerchantAdminService;
import org.paykesho.backoffice.merchants.enums.Constants;
import org.paykesho.backoffice.merchants.service.MerchantService;
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
                
                MerchantAdmin admin = memberAdminService.save(request, merchantAdmin, merchant);
                merchant.setContactPerson(admin);
                merchantService.save(merchant);
                
                if (isNewAdmin) {
                    redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Merchant Admin created successfully! Welcome email with login credentials has been sent to " + merchantAdmin.getEmail());
                } else {
                    redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Merchant Admin updated successfully: " + merchantAdmin.getFullName());
                }
            } else {
                throw new PaykeshoError("Merchant not found");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save merchant admin: " + e.getMessage());
        }

        return Constants.REDIRECT_MERCHANTS_VIEW + (merchant != null ? merchant.getId() : "");
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
