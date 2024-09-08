package org.paykesho.organizationportal.vouchers.controller;

import org.commons.entity.Voucher;
import org.commons.entity.organization.Organization;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.utils.PaykeshoError;
import org.paykesho.organizationportal.organizationadmins.service.OrganizationAdminService;
import org.paykesho.organizationportal.organizations.service.OrganizationService;
import org.paykesho.organizationportal.security.OrganizationAdminUserDetails;
import org.paykesho.organizationportal.settings.service.SettingService;
import org.paykesho.organizationportal.vouchers.service.VouchersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/vouchers")
public class VouchersController {

    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OrganizationAdminService organizationAdminService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private VouchersService vouchersService;

    @GetMapping("")
    public String showOrganizationTransactions(@AuthenticationPrincipal OrganizationAdminUserDetails loggedInUser, Model model) throws PaykeshoError {
        OrganizationAdmin merchantAdmin = organizationAdminService.getOrganizationAdminByEmail(loggedInUser.getOrganizationAdmin().getEmail());
        Organization organization = organizationService.getOrganizationByOrganizationAdmin(merchantAdmin);
        List<Voucher> voucherList = vouchersService.getOrganizationVouchers(organization);
        model.addAttribute("voucherList", voucherList);
        model.addAttribute("pageTitle", organization.getName() + " Vouchers");

        return "vouchers/vouchers";
    }

}
