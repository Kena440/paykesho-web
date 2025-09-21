package org.paykesho.clientportal.clients.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commons.entity.client.Client;
import org.commons.entity.organization.Organization;
import org.commons.entity.settings.Country;
import org.commons.enums.ClientType;
import org.paykesho.clientportal.clients.service.ClientService;
import org.paykesho.clientportal.clients.service.CreditScoreService;
import org.paykesho.clientportal.config.Utility;
import org.paykesho.clientportal.dashboard.service.ClientDashboardAnalyticsService;
import org.paykesho.clientportal.merchant.service.MerchantService;
import org.paykesho.clientportal.organizations.service.OrganizationService;
import org.paykesho.clientportal.security.ClientUserDetails;
import org.paykesho.clientportal.security.oauth.ClientOAuth2User;
import org.springframework.data.repository.query.Param;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/clients")
public class ClientController {

    // private final NotificationClient notificationClient; // Unused field

    private final ClientService clientService;

    private final CreditScoreService creditScoreService;

    private final MerchantService merchantService;

    private final OrganizationService organizationService;

    // private final SettingService settingService; // Unused field

    // private final VoucherService voucherService; // Unused field

    private final ClientDashboardAnalyticsService clientDashboardAnalyticsService;

    private static String getGreeting() {
        String greeting = null;
        LocalTime currentTime = LocalTime.now();

        if (currentTime.isBefore(LocalTime.NOON)) {
            greeting = "Good Morning";
        } else if (currentTime.isBefore(LocalTime.of(17, 0))) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }
        return greeting;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, @AuthenticationPrincipal ClientUserDetails loggedInUser) {
        try {
            log.debug("Loading client dashboard for user: {}",
                    loggedInUser != null ? loggedInUser.getClient().getEmail() : "unknown");

            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Client client = null;

            if (principal instanceof UserDetails && loggedInUser != null) {
                try {
                    client = clientService.getClientByEmail(loggedInUser.getClient().getEmail());
                } catch (Exception ex) {
                    log.warn("Failed to get client by email: {}", ex.getMessage());
                    model.addAttribute("errorMessage", "Failed to load client information.");
                    return "error/500";
                }

                String greeting = getGreeting();
                if (client != null) {
                    try {
                        BigDecimal availableBalance = clientService.getClientBalance(client).getAvailableBalance();
                        model.addAttribute("availableBalance", availableBalance);
                    } catch (Exception ex) {
                        log.warn("Failed to get client balance: {}", ex.getMessage());
                        model.addAttribute("availableBalance", BigDecimal.ZERO);
                    }

                    // Get comprehensive client analytics
                    try {
                        Map<String, Object> analytics = clientDashboardAnalyticsService.getClientAnalytics(client);
                        if (analytics != null) {
                            // Add all analytics to model
                            model.addAllAttributes(analytics);
                        } else {
                            log.warn("Analytics returned null for client: {}", client.getName());
                        }
                    } catch (Exception ex) {
                        log.warn("Failed to get client analytics: {}", ex.getMessage());
                    }

                    try {
                        int creditScore = creditScoreService.calculateCreditScore(client);
                        model.addAttribute("creditScore", creditScore);
                    } catch (Exception ex) {
                        log.warn("Failed to calculate credit score: {}", ex.getMessage());
                        model.addAttribute("creditScore", 0);
                    }
                }

                model.addAttribute("greeting", greeting);
                model.addAttribute("client", client);
            } else {
                log.warn("No authenticated user found for dashboard request");
                model.addAttribute("errorMessage", "Authentication required. Please log in.");
                return "security/login";
            }

            try {
                model.addAttribute("merchantList", merchantService.listAllEnabledMerchants());
            } catch (Exception ex) {
                log.warn("Failed to load merchant list: {}", ex.getMessage());
                model.addAttribute("merchantList", null);
            }

            model.addAttribute("pageTitle", "Dashboard");
            log.debug("Successfully loaded client dashboard");
            return "clients/dashboard";

        } catch (Exception ex) {
            log.error("Error loading client dashboard: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Failed to load dashboard. Please try again later.");
            return "error/500";
        }
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        List<Organization> organizationList = organizationService.organizationListEnabled();
        Client client = new Client();
        client.setEnabled(false);
        client.setClientType(ClientType.FORMAL);

        model.addAttribute("organizationList", organizationList);
        model.addAttribute("pageTitle", "Client Registration");
        model.addAttribute("client", client);
        return "register/register_form";
    }

    @PostMapping(value = "/save-client")
    public String signUpClient(Client client, Model model, HttpServletRequest request) {
        List<Organization> organizationList = organizationService.organizationListEnabled();
        model.addAttribute("organizationList", organizationList);
        model.addAttribute("pageTitle", "Client Registration");
        try {
            clientService.registerClient(client);
            // sendNotification(client, request);
            model.addAttribute("client", new Client()); // clear form
            model.addAttribute("successMessage", "Registration successful! Please check your email for verification.");
        } catch (Exception ex) {
            log.error("Registration failed", ex); // log full stack trace
            model.addAttribute("client", client); // keep user input
            String userMessage = "Registration failed. Please check your details and try again.";
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                String causeMsg = ex.getCause().getMessage();
                if (causeMsg.contains("constraint") || causeMsg.contains("duplicate")) {
                    userMessage = "A user with similar details already exists.";
                }
            }
            model.addAttribute("errorMessage", userMessage);
        }
        return "register/register_form";
    }

    @PostMapping(value = "/update-account")
    public String updateAccount(Client client, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        try {
            log.debug("Updating client account for email: {}", client.getEmail());

            clientService.updateClient(client);
            updateNameForAuthenticatedClient(client, request);
            redirectAttributes.addFlashAttribute("message", "Account update successful");

            log.debug("Successfully updated client account");
            return "redirect:/";

        } catch (Exception ex) {
            log.error("Error updating client account: {}", ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Account update failed. Please try again later.");
            return "redirect:/account";
        }
    }

    @GetMapping("/verify")
    public String verifyClient(@Param("code") String code, Model model) {
        try {
            log.debug("Verifying client with code: {}", code);

            boolean verified = clientService.verifyClient(code);
            log.debug("Client verification result: {}", verified);

            return "register/" + (verified ? "verify_success" : "verify_fail");

        } catch (Exception ex) {
            log.error("Error during client verification: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Verification failed. Please try again later.");
            return "error/500";
        }
    }

    @GetMapping("/account")
    public String viewClientAccount(Model model, HttpServletRequest httpServletRequest) {
        try {
            log.debug("Loading client account page");

            String pageTitle = "My Account";
            List<Country> countryList = null;
            Client client = null;

            try {
                countryList = clientService.countryList();
            } catch (Exception ex) {
                log.warn("Failed to load country list: {}", ex.getMessage());
            }

            String email = Utility.getEKeshofAuthenticatedClient(httpServletRequest);
            if (email != null) {
                try {
                    client = clientService.getClientByEmail(email);
                } catch (Exception ex) {
                    log.warn("Failed to get client by email: {}", ex.getMessage());
                }
            } else {
                log.warn("No authenticated client found for account request");
                model.addAttribute("errorMessage", "Authentication required. Please log in.");
                return "security/login";
            }

            model.addAttribute("pageTitle", pageTitle);
            model.addAttribute("countryList", countryList);
            model.addAttribute("client", client);
            log.debug("Successfully loaded client account page");
            return "clients/account";

        } catch (Exception ex) {
            log.error("Error loading client account page: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Failed to load account page. Please try again later.");
            return "error/500";
        }
    }

    private void updateNameForAuthenticatedClient(Client client, HttpServletRequest request) {
        Object principal = request.getUserPrincipal();

        if (principal instanceof UsernamePasswordAuthenticationToken || principal instanceof RememberMeAuthenticationToken) {
            ClientUserDetails clientUserDetails = getClientUserDetailsObject(principal);
            Client authClient = clientUserDetails.getClient();
            authClient.setName(client.getName());
        } else if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) principal;
            ClientOAuth2User clientOAuth2User = (ClientOAuth2User) oAuth2AuthenticationToken.getPrincipal();
            String clientFullName = client.getFullName();
            clientOAuth2User.setFullName(clientFullName);
        }
    }

    private ClientUserDetails getClientUserDetailsObject(Object principal) {
        ClientUserDetails clientUserDetails = null;
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
            clientUserDetails = (ClientUserDetails) token.getPrincipal();
        } else if (principal instanceof RememberMeAuthenticationToken) {
            // RememberMeAuthenticationToken token = (RememberMeAuthenticationToken) principal; // Unused variable
        }
        return clientUserDetails;
    }


}
