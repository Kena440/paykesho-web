package org.chaz.backoffice.assessors.controller;


import org.chaz.backoffice.assessors.repository.AssessorRepository;
import org.chaz.backoffice.assessors.service.AssessorService;
import org.chaz.backoffice.memberadmin.enums.Constants;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.chaz.backoffice.paging.PagingAndSortingParam;
import org.commons.entity.Assessor;
import org.commons.entity.settings.Country;
import org.commons.utils.AssessmentToolError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.List;

@Controller
@RequestMapping("/member-admins")
public class AssessorController {
    @Autowired
    private AssessorRepository assessorRepository;
    @Autowired
    private AssessorService assessorService;

    @GetMapping("")
    public String listFirstPage(Model model) {
        return Constants.REDIRECT_CUSTOMERS_PAGE_WITHOUT_KEYWORD;
    }

    @GetMapping("/page/{pageNum}")
    public String listByPage(Model model, @PathVariable(name = "pageNum") int pageNum, @PagingAndSortingParam(listName = "customerList", moduleURL = "/customers") PagingAndSortingHelper helper) throws AssessmentToolError {

        assessorService.listByPage(pageNum, helper);

        return "customers/customers";
    }

    @GetMapping("/{id}/enabled/{status}")
    public String updateCustomerEnabledStatus(@PathVariable(name = "id") Integer id, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes, Model model) throws AssessmentToolError {

        Assessor assessor = assessorRepository.get(id);
        assessorService.updateCustomerEnabledStatus(id, enabled);
        String status = enabled ? "enabled" : "has been disabled";
        String operationMSG = assessor.getFullName() + " has been " + status;
        redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        return Constants.REDIRECT_CUSTOMERS;

    }

    @GetMapping("/details/{id}")
    public String viewCustomer(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) throws AssessmentToolError {
        try {
            Assessor assessor = assessorService.get(id);
            model.addAttribute("customer", assessor);
            return "customers/customer_details_modal";
        } catch (AssessmentToolError fleetMgtError) {
            redirectAttributes.addFlashAttribute("message", fleetMgtError.getMessage());
            return Constants.REDIRECT_CUSTOMERS;
        }
    }

    @GetMapping("/edit/{id}")
    public String editCustomer(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws AssessmentToolError {
        if (id == null) {
            return Constants.REDIRECT_CUSTOMERS;
        }
        String pageTitle = "Edit Customer";

        try {
            Assessor assessor = assessorService.get(id);
            List<Country> countryList = assessorService.countryList();

            model.addAttribute("customer", assessor);
            model.addAttribute("countryList", countryList);
            model.addAttribute("pageTitle", pageTitle);

            return "customers/customer_form";
        } catch (AssessmentToolError membershipMgtError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, membershipMgtError.getMessage());
            return Constants.REDIRECT_CUSTOMERS;
        }

    }

    @PostMapping("/save")
    public String saveAdmin(Assessor assessor, HttpServletRequest request, RedirectAttributes redirectAttributes) throws AssessmentToolError, MessagingException, UnsupportedEncodingException {

            assessor.setAccountNonLocked(true);
            assessor.setFailedAttempt(0);
            Assessor admin = assessorService.save(request, assessor);


        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Admin Operation Successful: " + assessor.getFullName());
        return org.chaz.backoffice.assessors.enums.Constants.REDIRECT_TO_MEMBER + assessor.getId();
    }

    @GetMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws AssessmentToolError {
        if (id == null) {
            return Constants.REDIRECT_CUSTOMERS;
        }

        try {
            assessorService.delete(id);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "CUSTOMER deleted successfully");
        } catch (AssessmentToolError membershipMgtError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, membershipMgtError.getMessage());
        }
        return Constants.REDIRECT_CUSTOMERS;
    }

}
