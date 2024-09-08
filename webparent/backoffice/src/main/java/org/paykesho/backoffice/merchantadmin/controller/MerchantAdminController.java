package org.paykesho.backoffice.merchantadmin.controller;


import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.MerchantAdmin;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.merchantadmin.service.MerchantAdminService;
import org.paykesho.backoffice.merchants.enums.Constants;
import org.paykesho.backoffice.merchants.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

@Controller
@RequestMapping("/merchant-admin")
public class MerchantAdminController {
    @Autowired
    private MerchantAdminService memberAdminService;
    @Autowired
    private MerchantService merchantService;


    @PostMapping("/save")
    public String saveAdmin(MerchantAdmin merchantAdmin, HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError, MessagingException, UnsupportedEncodingException {
        Merchant merchant = merchantService.getMerchantById(Integer.valueOf(request.getParameter("merchantId")));
        if (merchant != null) {
            merchantAdmin.setAccountNonLocked(true);
            merchantAdmin.setFailedAttempt(0);
            MerchantAdmin admin = memberAdminService.save(request, merchantAdmin);
            merchant.setContactPerson(admin);
            merchantService.save(merchant);
        } else {
            throw new PaykeshoError("No institution found");
        }

        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Merchant Admin Operation Successful: " + merchantAdmin.getFullName());
        return Constants.REDIRECT_MERCHANTS_VIEW + merchant.getId();
    }

}
