package org.paykesho.backoffice.organizationadmin.controller;


import org.commons.entity.organization.Organization;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.organizationadmin.service.OrganizationAdminService;
import org.paykesho.backoffice.organizations.enums.Constants;
import org.paykesho.backoffice.organizations.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

@Controller
@RequestMapping("/organization-admin")
public class OrganizationAdminController {
    @Autowired
    private OrganizationAdminService organizationAdminService;
    @Autowired
    private OrganizationService organizationService;


    @PostMapping("/save")
    public String saveAdmin(OrganizationAdmin organizationAdmin, HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError, MessagingException, UnsupportedEncodingException {
        Organization organization = organizationService.getOrganizationById(Integer.valueOf(request.getParameter("organizationId")));
        if (organization != null) {
            organizationAdmin.setAccountNonLocked(true);
            organizationAdmin.setFailedAttempt(0);
            OrganizationAdmin admin = organizationAdminService.save(request, organizationAdmin);
            organization.setContactPerson(admin);
            organizationService.save(organization);
        } else {
            throw new PaykeshoError("No Organization found");
        }

        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization Admin Operation Successful: " + organizationAdmin.getFullName());
        return Constants.REDIRECT_ORGANIZATIONS_VIEW + organization.getId();
    }

}
