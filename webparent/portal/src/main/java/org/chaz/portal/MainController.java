package org.chaz.portal;

import org.chaz.portal.assessors.service.AssessorService;
import org.chaz.portal.facilities.service.FacilityService;
import org.chaz.portal.settings.service.SettingService;
import org.commons.entity.assessment.Assessor;
import org.commons.utils.AssessmentToolError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class MainController {

    @Autowired
    private AssessorService assessorService;
    @Autowired
    private FacilityService facilityService;
    @Autowired
    private SettingService settingService;

    @GetMapping("")
    public String viewHomePage(Model model, HttpServletRequest request) throws AssessmentToolError {
        Assessor assessor = assessorService.getAuthenticatedAssessor(request);

        int countTotalInterviews = 0;
        int countTotalAssessments = 0;

        model.addAttribute("countTotalInterviews", countTotalInterviews);
        model.addAttribute("countTotalAssessments", countTotalAssessments);

        return "index";
    }

    @GetMapping("/login")
    public String viewLoginPage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "security/login";
        }
        return "redirect:/";
    }

    @GetMapping("/sign-up")
    public String viewSignUpPage(Model model) {
        return "register/signup_home";
    }
}
