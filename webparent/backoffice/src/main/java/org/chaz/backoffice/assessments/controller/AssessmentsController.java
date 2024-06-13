package org.chaz.backoffice.assessments.controller;

import org.chaz.backoffice.assessments.enums.Constants;
import org.chaz.backoffice.assessments.service.AssessmentCodeService;
import org.chaz.backoffice.assessments.service.AssessmentsService;
import org.chaz.backoffice.assessors.service.AssessorService;
import org.chaz.backoffice.config.Utility;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.chaz.backoffice.paging.PagingAndSortingParam;
import org.chaz.backoffice.settings.email.EmailSettingBag;
import org.chaz.backoffice.settings.service.SettingService;
import org.commons.entity.assessment.*;
import org.commons.enums.FormTarget;
import org.commons.enums.FormType;
import org.commons.enums.QuestionType;
import org.commons.utils.AssessmentToolError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
    public String listFirstPage() {
        return Constants.REDIRECT_TO_Assessments_WITHOUT_KEYWORD;
    }

    @GetMapping("/page/{pageNum}")
    public String listAllAssessmentsByPage(@PagingAndSortingParam(listName = "assessmentList", moduleURL = "/assessments") PagingAndSortingHelper helper, @PathVariable("pageNum") int pageNum) throws AssessmentToolError {
        assessmentsService.listByPage(pageNum, helper);
        return "assessments/assessments";
    }

    @GetMapping("/new")
    public String newAssessment(Model model) {
        String pageTitle = "New Assessment";

        Assessment assessment = new Assessment();
        assessment.setEnabled(true);
        assessment.setAssessmentDate(new Date());

        model.addAttribute("assessment", assessment);
        model.addAttribute("pageTitle", pageTitle);
        return "assessments/assessment_form";
    }

    @PostMapping("/save")
    public String saveAssessment(Assessment assessment, RedirectAttributes redirectAttributes) throws AssessmentToolError {

        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        try {
            assessment.setAssessmentDate(dateFormatter.parse(assessment.getUpdatedTimeOnForm()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Assessment savedAssessment = assessmentsService.save(assessment);

        //todo add logic for survey if response is required

        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "assessment Operation Successful: " + savedAssessment.getName());
        return getRedirectURLtoAffectedAssessment(savedAssessment);
    }

    @GetMapping("/edit/{id}")
    public String editAssessment(@PathVariable(name = "id") Integer id, Model model) throws AssessmentToolError {
        if (id == null) {
            return Constants.REDIRECT_TO_Assessments;
        }
        String pageTitle = "Edit Assessment";
        Assessment assessment = assessmentsService.getAssessmentById(id);

        model.addAttribute("assessment", assessment);
        model.addAttribute("pageTitle", pageTitle);

        return "assessments/assessment_form";
    }

    @GetMapping("delete/{id}")
    public String deleteAssessment(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("no parameter id");
        }
        assessmentsService.delete(id);
        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "assessment deleted successfully");
        return Constants.REDIRECT_TO_Assessments;
    }

    @GetMapping("/{id}/enabled/{status}")
    public String updateAssessmentEnabledStatus(@PathVariable(name = "id") Integer id, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes, Model model) throws AssessmentToolError {

        if (id == null) {
            throw new AssessmentToolError("no parameter id");
        }

        try {
            Assessment assessment = assessmentsService.getAssessmentById(id);
            assessmentsService.updateAssessmentStatus(id, enabled);
            String status = enabled ? "enabled" : "has been disabled";
            String operationMSG = assessment.getName() + " has been " + status;
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        } catch (AssessmentToolError supremaError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, supremaError.getMessage());
        }
        return Constants.REDIRECT_TO_Assessments;

    }

    private String getRedirectURLtoAffectedAssessment(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("no assessment provided");
        }
        String assessmentName = assessment.getName();
        return Constants.REDIRECT_TO_Assessments_KEYWORD + assessmentName;
    }

    @GetMapping("/view/{id}")
    public String viewAssessment(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Assessment assessment = assessmentsService.getAssessmentById(id);
            List<AssessmentAssessor> assessmentAssessors = assessmentsService.getAssessmentAssessors(assessment);
            List<AssessmentQuestion> assessmentQuestions = assessmentsService.getAssessmentQuestions(assessment);
            List<AssessmentForm> assessmentForms = assessmentsService.getAssessmentForms(assessment);
            List<Assessor> assessorList = assessorService.assessorList();
            List<AssessmentInterview> assessmentInterviews = assessmentsService.findRecentInterviews(assessment.getId());
            long assessmentInterviewsCount = assessmentsService.countAssessmentInterviews(assessment.getId());

            model.addAttribute("assessment", assessment);
            model.addAttribute("assessmentAssessors", assessmentAssessors);
            model.addAttribute("assessmentInterviewsCount", assessmentInterviewsCount);
            model.addAttribute("assessmentInterviews", assessmentInterviews);
            model.addAttribute("assessmentQuestions", assessmentQuestions);
            model.addAttribute("assessmentForms", assessmentForms);
            model.addAttribute("assessorList", assessorList);
            model.addAttribute("assessmentLink", "https://assessment.chaz.domains/assessments/link/" + assessment.getId());
            model.addAttribute("qrCodeContent", "/events/generateQRCode?qrContent=" + "https://assessment.chaz.domains/assessments/link/" + assessment.getId());

            return "assessments/assessment_view";
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute("message", assessmentToolError.getMessage());
            return "redirect:/assessments";
        }
    }


    @GetMapping("/generateQRCode")
    public void generateQRCode(String qrContent, HttpServletResponse response) throws IOException {
        response.setContentType("image/png");
        byte[] qrCode = assessmentCodeService.generateQRCode(qrContent, 500, 500);
        OutputStream outputStream = response.getOutputStream();
        outputStream.write(qrCode);
    }

    @PostMapping("/save-assessor")
    public String saveAssessmentAssessor(RedirectAttributes redirectAttributes, HttpServletRequest request) throws AssessmentToolError {

        Assessment assessment = assessmentsService.getAssessmentById(Integer.valueOf(request.getParameter("assessmentId")));

        if (assessment == null) {
            throw new AssessmentToolError("No assessment");
        }

        if (assessment.isEnabled()) {
            String assessmentAssessor = request.getParameter("assessmentAssessor");
            Assessor assessorFromDB = assessorService.get(Integer.valueOf(assessmentAssessor));

            if (assessorFromDB == null) {
                throw new AssessmentToolError("No assessorFromDB");
            }

            AssessmentAssessor assessor = new AssessmentAssessor();
            assessor.setAssessment(assessment);
            assessor.setAssessor(assessorFromDB);
            assessor.setAssessmentRegId(String.valueOf(Instant.now().getEpochSecond()));
            assessor.setCreatedDate(new Date());

            AssessmentAssessor savedAssessmentAssessor = assessmentsService.saveAssessor(assessor);

            if (savedAssessmentAssessor.getAssessor().getEmail() != null) {
                sendEmailToContactPersonThread(savedAssessmentAssessor);
            }

            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Assessment Operation Successful: " + savedAssessmentAssessor.getAssessor().getName());
            return Constants.REDIRECT_TO_Assessment_ID + assessment.getId();
        } else {
            return Constants.REDIRECT_TO_Assessments;
        }

    }

    @PostMapping("/save-question")
    public String saveAssessmentQuestion(RedirectAttributes redirectAttributes, HttpServletRequest request) throws AssessmentToolError {

        String assessmentFormIdParam = request.getParameter("assessmentForm");
        String questionTypeParam = request.getParameter("questionType");
        String questionParam = request.getParameter("question");
        String questionSkippableParam = request.getParameter("questionSkippable");
        String questionNeedsVerificationParam = request.getParameter("questionNeedsVerification");
        String questionNeedsExplanationParam = request.getParameter("questionNeedsExplanation");
        String verificationQuestionParam = "";
        String questionVerificationInputTypeParam = "";

        // Check if any of the required parameters are empty
        if (StringUtils.isEmpty(assessmentFormIdParam) || StringUtils.isEmpty(questionTypeParam) || StringUtils.isEmpty(questionParam) || StringUtils.isEmpty(questionSkippableParam) || StringUtils.isEmpty(questionNeedsVerificationParam) || StringUtils.isEmpty(questionNeedsExplanationParam)) {
            throw new AssessmentToolError("One or more required parameters are empty");
        }

        // Convert parameters to appropriate types
        Integer assessmentFormId = Integer.valueOf(assessmentFormIdParam);
        QuestionType questionType = QuestionType.valueOf(questionTypeParam);
        String questionReq = questionParam;
        boolean questionSkippable = Boolean.parseBoolean(questionSkippableParam);
        boolean questionNeedsVerification = Boolean.parseBoolean(questionNeedsVerificationParam);
        boolean questionNeedsExplanation = Boolean.parseBoolean(questionNeedsExplanationParam);

        if (questionNeedsVerification) {
            verificationQuestionParam = request.getParameter("verificationQuestion");
            questionVerificationInputTypeParam = request.getParameter("verificationInputType");
        }

        AssessmentForm assessmentForm = assessmentsService.getAssessmentFormById(assessmentFormId);

        if (assessmentForm == null) {
            throw new AssessmentToolError("No assessmentForm found for ID: " + assessmentFormId);
        }

        if (assessmentForm.getAssessment().isEnabled()) {
            AssessmentQuestion question = new AssessmentQuestion();
            question.setAssessmentForm(assessmentForm);
            question.setQuestionType(questionType);
            question.setQuestion(questionReq);
            question.setSkippable(questionSkippable);
            question.setNeedsVerification(questionNeedsVerification);
            question.setNeedsExplanation(questionNeedsExplanation);

            if (questionNeedsVerification) {
                question.setVerificationOption(questionVerificationInputTypeParam);
                question.setVerificationQuestion(verificationQuestionParam);
            }

            question.setCreatedDate(new Date());

            AssessmentQuestion savedAssessmentQuestion = assessmentsService.saveQuestion(question);

            // Handle options if applicable
            if (questionType == QuestionType.MULTIPLE_CHOICE || questionType == QuestionType.CHECKBOX) {
                String[] options = request.getParameterValues("options");
                if (options != null) {
                    for (String optionText : options) {
                        AssessmentQuestionOption option = new AssessmentQuestionOption();
                        option.setQuestion(savedAssessmentQuestion);
                        option.setOptionText(optionText);
                        assessmentsService.saveQuestionOption(option);
                    }
                }
            }

            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Assessment Operation Successful: " + savedAssessmentQuestion.getAssessmentForm().getAssessment().getName());
            return Constants.REDIRECT_TO_Assessment_ID + savedAssessmentQuestion.getAssessmentForm().getAssessment().getId();
        } else {
            return Constants.REDIRECT_TO_Assessments;
        }
    }

    @PostMapping("/save-form")
    public String saveAssessmentForm(RedirectAttributes redirectAttributes, HttpServletRequest request) throws AssessmentToolError {
        Assessment assessment = assessmentsService.getAssessmentById(Integer.valueOf(request.getParameter("assessmentId")));

        if (assessment == null) {
            throw new AssessmentToolError("No assessment");
        }

        if (assessment.isEnabled()) {
            String formName = request.getParameter("formName");
            String formType = request.getParameter("formType");
            String formTarget = request.getParameter("formTarget");

            AssessmentForm question = new AssessmentForm();
            question.setAssessment(assessment);
            question.setFormName(formName);
            question.setFormType(FormType.valueOf(formType));
            question.setFormTarget(FormTarget.valueOf(formTarget));
            question.setCreatedDate(new Date());

            AssessmentForm savedAssessmentForm = assessmentsService.saveForm(question);

            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Assessment Operation Successful: " + savedAssessmentForm.getAssessment().getName());
            return Constants.REDIRECT_TO_Assessment_ID + savedAssessmentForm.getAssessment().getId();
        } else {
            return Constants.REDIRECT_TO_Assessments;
        }
    }

    @GetMapping("/interview/view/{id}")
    public String viewAssessmentInterview(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            AssessmentInterview assessmentInterview = assessmentsService.getAssessmentInterviewById(id);
            List<AssessmentQuestionResponse> assessmentQuestionResponses = assessmentsService.getAssessmentQuestionResponseByInterviewId(assessmentInterview);
            List<AssessmentQuestion> assessmentQuestions = assessmentsService.getAssessmentQuestionsByForm(assessmentInterview.getAssessmentForm());
            Map<AssessmentQuestion, List<AssessmentQuestionResponse>> questionResponseMap = new LinkedHashMap<>();
            for (AssessmentQuestion question : assessmentQuestions) {
                List<AssessmentQuestionResponse> responses = assessmentQuestionResponses.stream().filter(response -> response.getAssessmentQuestion().getId().equals(question.getId())).collect(Collectors.toList());
                questionResponseMap.put(question, responses);
            }

            List<AssessmentQuestion> assessmentVerificationQuestions = assessmentsService.getVerifiedAssessmentQuestions(assessmentInterview.getAssessmentForm());

            model.addAttribute("assessmentInterview", assessmentInterview);
            model.addAttribute("assessmentQuestions", assessmentQuestions);
            model.addAttribute("questionResponseMap", questionResponseMap);
            model.addAttribute("assessmentVerificationQuestions", assessmentVerificationQuestions.size());
            model.addAttribute("pageTitle", "Interview View");

            return "assessments/interview_view";
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute("message", assessmentToolError.getMessage());
            return "redirect:/assessments";
        }
    }

    @GetMapping("/interview/verification/view/{id}")
    public String viewAssessmentInterviewVerification(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            AssessmentInterview assessmentInterview = assessmentsService.getAssessmentInterviewById(id);
            List<AssessmentQuestionVerificationResponse> assessmentQuestionVerificationResponses = assessmentsService.getAssessmentQuestionVerificationResponseByInterviewId(assessmentInterview);
            List<AssessmentQuestion> assessmentQuestions = assessmentsService.getVerifiedAssessmentQuestions(assessmentInterview.getAssessmentForm());
            Map<AssessmentQuestion, List<AssessmentQuestionVerificationResponse>> questionResponseMap = new LinkedHashMap<>();
            for (AssessmentQuestion question : assessmentQuestions) {
                List<AssessmentQuestionVerificationResponse> responses = assessmentQuestionVerificationResponses.stream().filter(response -> response.getAssessmentQuestion().getId().equals(question.getId())).collect(Collectors.toList());
                questionResponseMap.put(question, responses);
            }

            model.addAttribute("assessmentInterview", assessmentInterview);
            model.addAttribute("questionResponseMap", questionResponseMap);
            model.addAttribute("assessmentVerificationQuestions", assessmentQuestions);
            model.addAttribute("pageTitle", "Interview Verification View");

            return "assessments/interview_verification_view";
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute("message", assessmentToolError.getMessage());
            return "redirect:/assessments";
        }
    }

    @GetMapping("/form/reports/{id}")
    public String viewAssessmentFormReport(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) throws AssessmentToolError {
        try {
            // Fetch assessment form and related data in bulk
            AssessmentForm assessmentForm = assessmentsService.getAssessmentFormById(id);
            List<AssessmentQuestion> allQuestions = assessmentsService.getAssessmentQuestionsByForm(assessmentForm);
            List<AssessmentInterview> assessmentInterviews = assessmentsService.getAssessmentInterviews(assessmentForm.getAssessment());

            // Filter questions
            List<AssessmentQuestion> filteredQuestions = allQuestions.stream()
                    .filter(q -> q.getQuestionType().equals(QuestionType.MULTIPLE_CHOICE) ||
                            q.getQuestionType().equals(QuestionType.CHECKBOX) ||
                            q.getQuestionType().equals(QuestionType.PRODUCT))
                    .collect(Collectors.toList());

            // Fetch all responses for the relevant interviews in one call
            List<AssessmentQuestionResponse> allResponses = assessmentsService.getAssessmentQuestionResponsesByInterviews(assessmentInterviews);

            // Group responses by question
            Map<Integer, List<AssessmentQuestionResponse>> responsesByQuestionId = allResponses.stream()
                    .collect(Collectors.groupingBy(response -> response.getAssessmentQuestion().getId()));

            // Prepare the maps for response counts and distributions
            Map<AssessmentQuestion, Long> responseCountPerQuestion = new LinkedHashMap<>();
            Map<AssessmentQuestion, Map<String, Long>> distributionPerQuestion = new LinkedHashMap<>();

            for (AssessmentQuestion question : filteredQuestions) {
                List<AssessmentQuestionResponse> responses = responsesByQuestionId.getOrDefault(question.getId(), Collections.emptyList());

                // Response count
                responseCountPerQuestion.put(question, (long) responses.size());

                // Distribution of answers
                Map<String, Long> distribution = responses.stream()
                        .collect(Collectors.groupingBy(AssessmentQuestionResponse::getAnswer, Collectors.counting()));
                distributionPerQuestion.put(question, distribution);
            }

            // Set model attributes
            model.addAttribute("responseCountPerQuestion", responseCountPerQuestion);
            model.addAttribute("distributionPerQuestion", distributionPerQuestion);
            model.addAttribute("assessmentForm", assessmentForm);
            model.addAttribute("pageTitle", "Form Reports");

            return "assessments/reports_view";
        } catch (AssessmentToolError assessmentToolError) {
            redirectAttributes.addFlashAttribute("message", assessmentToolError.getMessage());
            return "redirect:/assessments";
        }
    }

    private void sendEmailToContactPersonThread(AssessmentAssessor assessmentAssessor) {
        new Thread(() -> {
            try {
                sendEmail(assessmentAssessor);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }).start();
    }

    private void sendEmail(AssessmentAssessor assessmentAssessor) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);
        String feedback = "Congratulations!, you have successfully registered as an assessor for " + assessmentAssessor.getAssessment().getName();

        String toAddress = assessmentAssessor.getAssessor().getEmail();
        String subject = "Registration " + assessmentAssessor.getAssessment().getName();
        String content = "<p>Greetings from CHAZ,</p>" + "<p>\"" + feedback + "\"</p>" + "<br>" + "<p>Assessment Reg ID #" + assessmentAssessor.getAssessmentRegId() + ".</p>" + "<p>Thank you.</p>";

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBags.getFromAddress(), emailSettingBags.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        helper.setText(content, true);

        mailSender.send(mimeMessage);
    }

    private void sendEmailWithMessage(AssessmentAssessor assessmentAssessor, String message) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = assessmentAssessor.getAssessor().getEmail();
        String subject = "Updates: " + assessmentAssessor.getAssessment().getName();
        String content = "<p>Greetings from CHAZ,</p>" + "<p>\"" + message + "\"</p>" + "<br>" + "<p>Event Reg ID #" + assessmentAssessor.getAssessmentRegId() + ".</p>" + "<p>Thank you.</p>";

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBags.getFromAddress(), emailSettingBags.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        helper.setText(content, true);

        mailSender.send(mimeMessage);
    }

}
