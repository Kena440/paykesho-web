package org.chaz.backoffice.assessors.controller;


import org.chaz.backoffice.assessors.enums.Constants;
import org.chaz.backoffice.assessors.service.AssessorService;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.chaz.backoffice.paging.PagingAndSortingParam;
import org.commons.entity.assessment.Assessor;
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

@Controller
@RequestMapping("/assessors")
public class AssessorController {
    @Autowired
    private AssessorService assessorService;

    @GetMapping("")
    public String listFirstPage(Model model) {
        return Constants.REDIRECT_ASSESSORS_PAGE_1_SORT_FIELD_FIRST_NAME_SORT_DIR_ASC;
    }

    @GetMapping("/page/{pageNum}")
    public String listByPage(Model model, @PathVariable(name = "pageNum") int pageNum, @PagingAndSortingParam(listName = "assessorList", moduleURL = "/assessors") PagingAndSortingHelper helper) throws AssessmentToolError {

        assessorService.listByPage(pageNum, helper);

        return "assessors/assessors";
    }

    @GetMapping("/{id}/enabled/{status}")
    public String updateAssessorEnabledStatus(@PathVariable(name = "id") Integer id, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes) throws AssessmentToolError {

        Assessor assessor = assessorService.get(id);
        assessorService.updateAssessorEnabledStatus(id, enabled);
        String status = enabled ? "enabled" : "has been disabled";
        String operationMSG = assessor.getFullName() + " has been " + status;
        redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        return Constants.REDIRECT_ASSESSORS;

    }

    @GetMapping("/details/{id}")
    public String viewAssessor(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Assessor assessor = assessorService.get(id);
            model.addAttribute("customer", assessor);
            return "assessors/assessor_details_modal";
        } catch (AssessmentToolError fleetMgtError) {
            redirectAttributes.addFlashAttribute("message", fleetMgtError.getMessage());
            return Constants.REDIRECT_ASSESSORS;
        }
    }

    @GetMapping("/edit/{id}")
    public String editAssessor(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) {
        if (id == null) {
            return Constants.REDIRECT_ASSESSORS;
        }
        String pageTitle = "Edit Assessor";

        try {
            Assessor assessor = assessorService.get(id);

            model.addAttribute("assessor", assessor);
            model.addAttribute("pageTitle", pageTitle);

            return "assessors/assessor_form";
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, assessmentToolError.getMessage());
            return Constants.REDIRECT_ASSESSORS;
        }

    }

    @PostMapping("/save")
    public String saveAdmin(Assessor assessor, HttpServletRequest request, RedirectAttributes redirectAttributes) throws MessagingException, UnsupportedEncodingException {

        assessor.setAccountNonLocked(true);
        assessor.setFailedAttempt(0);
        Assessor admin = assessorService.save(request, assessor);

        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "assessor Operation Successful: " + admin.getFullName());
        return Constants.REDIRECT_ASSESSORS;
    }

    @GetMapping("/delete/{id}")
    public String deleteAssessor(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) {
        if (id == null) {
            return Constants.REDIRECT_ASSESSORS;
        }

        try {
            assessorService.delete(id);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "assessor deleted successfully");
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, assessmentToolError.getMessage());
        }
        return Constants.REDIRECT_ASSESSORS;
    }

    @GetMapping("/new")
    public String newAssessor(Model model) {
        String pageTitle = "New Assessor";
        Assessor assessor = new Assessor();
        assessor.setEnabled(true);
        model.addAttribute("assessor", assessor);
        model.addAttribute("pageTitle", pageTitle);
        return "assessors/assessor_form";
    }
}
