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
import org.commons.enums.QuestionType;
import org.commons.utils.AssessmentToolError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
import java.util.Date;
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
            List<AssessmentFeedback> assessmentFeedbacks = assessmentsService.getAssessmentFeedback(assessment);
            List<AssessmentQuestion> assessmentQuestions = assessmentsService.getAssessmentQuestions(assessment);
            List<Assessor> assessorList = assessorService.assessorList();
            model.addAttribute("assessment", assessment);
            model.addAttribute("assessmentAssessors", assessmentAssessors);
            model.addAttribute("assessmentFeedbacks", assessmentFeedbacks);
            model.addAttribute("assessmentQuestions", assessmentQuestions);
            model.addAttribute("assessorList", assessorList);
            model.addAttribute("assessmentLink", "https://assessment.chaz.domains/assessments/link/" + assessment.getId());
            model.addAttribute("qrCodeContent", "/events/generateQRCode?qrContent=" + "https://assessment.chaz.domains/assessments/link/" + assessment.getId());

            return "assessments/assessment_view";
        } catch (AssessmentToolError membershipMgtError) {
            redirectAttributes.addFlashAttribute("message", membershipMgtError.getMessage());
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

        Assessment assessment = assessmentsService.getAssessmentById(Integer.valueOf(request.getParameter("assessmentId")));

        if (assessment == null) {
            throw new AssessmentToolError("No assessment");
        }

        if (assessment.isEnabled()) {
            String questionType = request.getParameter("questionType");
            String questionFromReq = request.getParameter("question");
            String questionSkippable = request.getParameter("questionSkippable");

            AssessmentQuestion question = new AssessmentQuestion();
            question.setAssessment(assessment);
            question.setQuestionType(QuestionType.valueOf(questionType));
            question.setQuestion(questionFromReq);
            question.setSkippable(Boolean.parseBoolean(questionSkippable));
            question.setCreatedDate(new Date());

            AssessmentQuestion savedAssessmentQuestion = assessmentsService.saveQuestion(question);

            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Assessment Operation Successful: " + savedAssessmentQuestion.getAssessment().getName());
            return Constants.REDIRECT_TO_Assessment_ID + assessment.getId();
        } else {
            return Constants.REDIRECT_TO_Assessments;
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
