package org.paykesho.clientportal.security;


import org.commons.entity.client.Client;
import org.paykesho.clientportal.clients.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.paykesho.clientportal.kyc.KycStatusService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private ClientService clientService;
    @Autowired
    private KycStatusService kycStatusService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("email");
        Client client = clientService.getClientByEmail(email);

        if (client != null && !client.isEnabled()) {
            // Only tell someone who knows the password that the account awaits KYC
            // review, so the message does not reveal which e-mails are registered.
            String password = request.getParameter("password");
            String pending = kycStatusService.pendingMessage(client.getId());
            if (pending != null && password != null && client.getPassword() != null
                    && passwordEncoder.matches(password, client.getPassword())) {
                exception = new DisabledException(pending);
            } else {
                exception = new BadCredentialsException("Bad credentials");
            }
        } else if (client != null) {
            if (client.isEnabled() && client.isAccountNonLocked()) {
                if (client.getFailedAttempt() < ClientService.MAX_FAILED_ATTEMPTS - 1) {
                    clientService.increaseFailedAttempts(client);
                    //todo code for email to user on failed login
                } else {
                    clientService.lock(client);
                    exception = new LockedException("Your account has been locked due to 3 failed attempts."
                            + " It will be unlocked after 24 hours.");
                    //todo code for email to user on failed login
                }
            } else if (!client.isAccountNonLocked()) {
                if (clientService.unlockWhenTimeExpired(client)) {
                    exception = new LockedException("Your account has been unlocked. Please try to login again.");
                    //todo code for email to user on failed login
                }
            }
        }

        super.setDefaultFailureUrl("/login?error");
        super.onAuthenticationFailure(request, response, exception);
    }

}
