package org.chaz.portal.assessments.controller;

import org.chaz.portal.assessments.service.AssessmentCodeService;
import org.chaz.portal.assessments.service.AssessmentsService;
import org.chaz.portal.assessors.service.AssessorService;
import org.chaz.portal.settings.service.SettingService;
import org.commons.entity.assessment.Assessment;
import org.commons.entity.assessment.AssessmentInterview;
import org.commons.entity.assessment.AssessmentQuestion;
import org.commons.entity.assessment.Assessor;
import org.commons.utils.AssessmentToolError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/assessments")
public class AssessmentsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssessmentsController.class);
    @Autowired
    private AssessmentsService assessmentsService;
    @Autowired
    private AssessmentCodeService assessmentCodeService;
    @Autowired
    private AssessorService assessorService;
    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String listFirstPage(Model model, HttpServletRequest request) throws AssessmentToolError {
        return listAllAssessmentsByPage(model, request, 1, "assessmentDate", "desc", null);
    }

    @GetMapping("/page/{pageNum}")
    public String listAllAssessmentsByPage(Model model, HttpServletRequest request, @PathVariable(name = "pageNum") int pageNum, String sortField, String sortDir, String assessmentKeyword) {
        Assessor assessor = assessorService.getAuthenticatedAssessor(request);
        Page<Assessment> page = assessmentsService.listForAssessorByPage(assessor, pageNum, sortField, sortDir, assessmentKeyword);
        List<Assessment> assessmentList = page.getContent();

        int pageSize = page.getSize();

        long startCount = (long) (pageNum - 1) * 10 + 1;
        long endCount = startCount + pageSize - 1;

        if (endCount > page.getTotalElements()) {
            endCount = page.getTotalElements();
        }

        int previousPage = pageNum - 1;
        int nextPage = pageNum + 1;
        if (previousPage == 0) {
            previousPage = 1;
        }
        if (nextPage > page.getTotalPages()) {
            nextPage = page.getTotalPages();
        }

        String reverseSortDir = sortDir.equals("asc") ? "desc" : "asc";

        model.addAttribute("keyword", assessmentKeyword);
        model.addAttribute("currentPage", pageNum);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("previousPage", previousPage);
        model.addAttribute("nextPage", nextPage);
        model.addAttribute("startCount", startCount);
        model.addAttribute("endCount", endCount);
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("moduleURL", "/assessments");
        model.addAttribute("reverseSortDir", reverseSortDir);
        model.addAttribute("pageTitle", "Assessments");
        model.addAttribute("assessor", assessor);
        model.addAttribute("assessmentList", assessmentList);

        return "assessments/assessments";
    }

    @GetMapping("/view/{id}")
    public String viewAssessment(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Assessment assessment = assessmentsService.getAssessmentById(id);
            List<AssessmentQuestion> assessmentQuestions = assessmentsService.getAssessmentQuestions(assessment);
            List<AssessmentInterview> assessmentInterviews = new ArrayList<>();
            model.addAttribute("assessment", assessment);
            model.addAttribute("assessmentQuestions", assessmentQuestions);
            model.addAttribute("assessmentInterviews", assessmentInterviews);
            model.addAttribute("assessmentLink", "https://assessment.chaz.domains/assessments/link/" + assessment.getId());
            model.addAttribute("qrCodeContent", "/events/generateQRCode?qrContent=" + "https://assessment.chaz.domains/assessments/link/" + assessment.getId());

            return "assessments/assessment_view";
        } catch (AssessmentToolError membershipMgtError) {
            redirectAttributes.addFlashAttribute("message", membershipMgtError.getMessage());
            return "redirect:/assessments";
        }
    }

}
