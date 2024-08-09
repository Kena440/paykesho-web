package org.paykesho.clientportal.security;

import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class DatabaseLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

/*    @Autowired
    private AssessorService assessorService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {

        AssessorUserDetails assessorUserDetails = (AssessorUserDetails) authentication.getPrincipal();
        Assessor assessor = assessorUserDetails.getAssessor();

        assessorService.updateAuthenticationType(assessor, AuthenticationType.DATABASE);
        if (assessor.getFailedAttempt() > 0) {
            assessorService.resetFailedAttempts(assessor.getEmail());
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }*/
}
