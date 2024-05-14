package org.chaz.portal.security;


import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

/*    @Autowired
    private AssessorService assessorService;*/

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
/*        String email = request.getParameter("email");
        Assessor assessor = assessorService.getAssessorByEmail(email);

        if (assessor != null) {
            if (assessor.isEnabled() && assessor.isAccountNonLocked()) {
                if (assessor.getFailedAttempt() < AssessorService.MAX_FAILED_ATTEMPTS - 1) {
                    assessorService.increaseFailedAttempts(assessor);

                    //todo code for email to user on failed login
                } else {
                    assessorService.lock(assessor);
                    exception = new LockedException("Your account has been locked due to 3 failed attempts."
                            + " It will be unlocked after 24 hours.");
                    //todo code for email to user on failed login
                }
            } else if (!assessor.isAccountNonLocked()) {
                if (assessorService.unlockWhenTimeExpired(assessor)) {
                    exception = new LockedException("Your account has been unlocked. Please try to login again.");
                    //todo code for email to user on failed login
                }
            }

        }

        super.setDefaultFailureUrl("/login?error");
        super.onAuthenticationFailure(request, response, exception);*/
    }

}
