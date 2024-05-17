package org.chaz.portal.assessments.controller;

import org.chaz.portal.assessments.dto.AssessmentResponseDTO;
import org.chaz.portal.assessments.service.AssessmentCodeService;
import org.chaz.portal.assessments.service.AssessmentsService;
import org.chaz.portal.assessors.service.AssessorService;
import org.chaz.portal.facilities.service.FacilityService;
import org.commons.entity.assessment.*;
import org.commons.entity.facilities.Facility;
import org.commons.enums.QuestionType;
import org.commons.utils.AssessmentToolError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
    private FacilityService facilityService;
    private static final Logger logger = LoggerFactory.getLogger(AssessmentsController.class);


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
            List<AssessmentForm> assessmentForms = assessmentsService.getAssessmentForms(assessment);
            List<AssessmentInterview> assessmentInterviews = assessmentsService.getAssessmentInterviews(assessment);
            List<Facility> facilityList = facilityService.getAssessmentFacilities();
            model.addAttribute("facilityList", facilityList);
            model.addAttribute("assessment", assessment);
            model.addAttribute("assessmentQuestions", assessmentQuestions);
            model.addAttribute("assessmentInterviews", assessmentInterviews);
            model.addAttribute("assessmentForms", assessmentForms);
            model.addAttribute("assessmentLink", "https://assessment.chaz.domains/assessments/link/" + assessment.getId());
            model.addAttribute("qrCodeContent", "/events/generateQRCode?qrContent=" + "https://assessment.chaz.domains/assessments/link/" + assessment.getId());

            return "assessments/assessment_view";
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute("message", assessmentToolError.getMessage());
            return "redirect:/assessments";
        }
    }

    @PostMapping("/interview/start")
    public String startAssessmentInterview(HttpServletRequest request, Model model) throws AssessmentToolError {
        Assessment assessment = assessmentsService.getAssessmentById(Integer.valueOf(request.getParameter("assessmentId")));
        if (assessment == null) {
            throw new AssessmentToolError("No assessment");
        }
        if (!assessment.isEnabled()) {
            throw new AssessmentToolError("assessment not enabled");
        }
        AssessmentForm assessmentForm = assessmentsService.getAssessmentFormById(Integer.valueOf(request.getParameter("assessmentForm")));
        if (assessmentForm == null) {
            throw new AssessmentToolError("No assessment form");
        }

        String respondentName = request.getParameter("respondentName");
        String respondentJobTitle = request.getParameter("respondentJobTitle");
        String respondentPhoneNumber = request.getParameter("respondentPhoneNumber");
        Facility facility = facilityService.getFacilityById(Integer.valueOf(request.getParameter("facility")));
        if (facility == null) {
            throw new AssessmentToolError("No facility");
        }

        AssessmentInterview assessmentInterview = new AssessmentInterview();
        assessmentInterview.setRespondentName(respondentName);
        assessmentInterview.setAssessmentForm(assessmentForm);
        assessmentInterview.setRespondentJobTitle(respondentJobTitle);
        assessmentInterview.setRespondentPhoneNumber(respondentPhoneNumber);
        assessmentInterview.setFacility(facility);
        assessmentInterview.setCreatedDate(new Date());
        assessmentInterview.setEndTime("12:00");
        assessmentInterview.setStartTime("10:00");
        assessmentInterview.setCompleted(false);

        AssessmentInterview savedAssessmentInterview = assessmentsService.saveInterview(assessmentInterview);

        List<AssessmentQuestion> assessmentQuestions = getAssessmentQuestions(assessmentForm);

        model.addAttribute("assessmentForm", assessmentForm);
        model.addAttribute("assessmentInterview", savedAssessmentInterview);
        model.addAttribute("assessmentQuestions", assessmentQuestions);
        return "assessments/assessment_form";
    }

    @GetMapping("/interview/continue/{id}")
    public String continueAssessmentInterview(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes, HttpServletRequest request) throws AssessmentToolError {

        AssessmentInterview assessmentInterview = assessmentsService.getAssessmentInterviewById(id);
        if (assessmentInterview == null) {
            throw new AssessmentToolError("No assessment Interview");
        }

        AssessmentForm assessmentForm = assessmentInterview.getAssessmentForm();

        List<AssessmentQuestion> assessmentQuestions = getAssessmentQuestions(assessmentForm);

        model.addAttribute("assessmentForm", assessmentForm);
        model.addAttribute("assessmentInterview", assessmentInterview);
        model.addAttribute("assessmentQuestions", assessmentQuestions);
        return "assessments/assessment_form";
    }

    @PostMapping("/interview/submit")
    public ResponseEntity<String> submitAssessment(@RequestBody AssessmentResponseDTO assessmentResponse) {
        String interviewId = assessmentResponse.getInterviewId();
        Map<String, Object> responses = assessmentResponse.getResponses();

        try {
            AssessmentInterview assessmentInterview = assessmentsService.getAssessmentInterviewById(Integer.valueOf(interviewId));
            if (assessmentInterview == null) {
                throw new AssessmentToolError("Interview not found for ID: " + interviewId);
            }

            int totalQuestions = getAssessmentQuestions(assessmentInterview.getAssessmentForm()).size();
            int answeredQuestions = 0;

            for (Map.Entry<String, Object> entry : responses.entrySet()) {
                String questionIdString = entry.getKey();
                Object answer = entry.getValue();

                AssessmentQuestion assessmentQuestion = assessmentsService.getAssessmentQuestionById(Integer.parseInt(questionIdString));
                if (assessmentQuestion == null) {
                    throw new AssessmentToolError("Assessment question not found for ID: " + questionIdString);
                }

                if (answer instanceof String) {
                    AssessmentQuestionResponse response = new AssessmentQuestionResponse();
                    response.setAssessmentQuestion(assessmentQuestion);
                    response.setAnswer((String) answer);
                    response.setAssessmentInterview(assessmentInterview);
                    assessmentsService.saveInterviewResponse(response);
                    answeredQuestions++;
                } else if (answer instanceof List) {
                    List<String> answerList = (List<String>) answer;
                    for (String value : answerList) {
                        AssessmentQuestionResponse checkboxResponse = new AssessmentQuestionResponse();
                        checkboxResponse.setAssessmentQuestion(assessmentQuestion);
                        checkboxResponse.setAnswer(value);
                        checkboxResponse.setAssessmentInterview(assessmentInterview);
                        assessmentsService.saveInterviewResponse(checkboxResponse);
                        answeredQuestions++;
                    }
                }
            }

            if (answeredQuestions == totalQuestions) {
                assessmentInterview.setCompleted(true);
                assessmentsService.saveInterview(assessmentInterview);
            }

            return ResponseEntity.ok("Assessment submitted successfully.");
        } catch (NumberFormatException | AssessmentToolError e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error submitting assessment: " + e.getMessage());
        }
    }

    private List<AssessmentQuestion> getAssessmentQuestions(AssessmentForm assessmentForm) throws AssessmentToolError {
        List<AssessmentQuestion> assessmentQuestions = assessmentsService.getAssessmentQuestionsByForm(assessmentForm);
        for (AssessmentQuestion question : assessmentQuestions) {
            if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE || question.getQuestionType() == QuestionType.CHECKBOX) {
                List<AssessmentQuestionOption> options = assessmentsService.getOptionsByQuestion(question);
                question.setOptions(options);
            }
        }
        return assessmentQuestions;
    }
}
