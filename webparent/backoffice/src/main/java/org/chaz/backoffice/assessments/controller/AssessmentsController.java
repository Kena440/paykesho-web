package org.chaz.backoffice.assessments.controller;

import org.chaz.backoffice.assessments.enums.Constants;
import org.chaz.backoffice.assessments.service.AssessmentCodeService;
import org.chaz.backoffice.assessments.service.AssessmentsService;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.chaz.backoffice.paging.PagingAndSortingParam;
import org.chaz.backoffice.settings.service.SettingService;
import org.chaz.backoffice.utils.AmazonS3Util;
import org.commons.entity.Assessment;
import org.commons.utils.AssessmentToolError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Controller
@RequestMapping("/assessments")
public class AssessmentsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssessmentsController.class);
    @Autowired
    private AssessmentsService assessmentsService;
    @Autowired
    private AssessmentCodeService assessmentCodeService;
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
    public String saveAssessment(Assessment assessment, RedirectAttributes redirectAttributes, @RequestParam("image") MultipartFile multipartFile) throws IOException, AssessmentToolError {

        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        try {
            assessment.setAssessmentDate(dateFormatter.parse(assessment.getUpdatedTimeOnForm()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Assessment savedAssessment = assessmentsService.save(assessment);

        //todo add logic for survey if response is required

        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "assessment Operation Successful: " + assessment.getName());
        return getRedirectURLtoAffectedAssessment(assessment);
    }

    @GetMapping("/edit/{id}")
    public String editAssessment(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws AssessmentToolError {
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

        String assessmentPhotosDir = "assessment_photos/" + id;
        AmazonS3Util.removeFolder(assessmentPhotosDir);
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

            model.addAttribute("assessment", assessment);

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


}
