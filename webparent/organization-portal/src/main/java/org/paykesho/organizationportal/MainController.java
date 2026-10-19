package org.paykesho.organizationportal;

import org.commons.entity.organization.Organization;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.entity.settings.Country;
import org.paykesho.organizationportal.dashboard.service.OrganizationDashboardAnalyticsService;
import org.paykesho.organizationportal.organizationadmins.service.OrganizationAdminService;
import org.paykesho.organizationportal.organizations.service.OrganizationService;
import org.paykesho.organizationportal.settings.service.SettingService;
import org.paykesho.organizationportal.vouchers.service.VouchersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    @Lazy
    private OrganizationAdminService organizationAdminService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private VouchersService voucherService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private OrganizationDashboardAnalyticsService organizationDashboardAnalyticsService;

    @GetMapping("")
    public String viewHomePage(Model model, HttpServletRequest request) {
        try {
            logger.debug("Loading organization portal dashboard for request: {}", request.getRequestURI());

            OrganizationAdmin organizationAdmin = organizationAdminService.getAuthenticatedOrganizationAdmin(request);
            if (organizationAdmin == null) {
                logger.warn("No authenticated organization admin found for request");
                model.addAttribute("errorMessage", "Authentication required. Please log in.");
                return "security/login";
            }

            // CRASH POINT #1 FIX: Safe organization retrieval
            Organization organization = organizationService.getOrganizationByOrganizationAdmin(organizationAdmin);
            if (organization == null) {
                logger.error("No organization found for authenticated admin: {} (ID: {})",
                        organizationAdmin.getEmail(), organizationAdmin.getId());
                model.addAttribute("errorMessage",
                        "Your account is not properly linked to an organization. Please contact support.");
                return "error/500";
            }

            logger.debug("Found organization: {} for admin: {}", organization.getName(), organizationAdmin.getEmail());

            Country country = settingService.getCountryByName("Zambia");

            // CRASH POINT #2 FIX: Safe analytics retrieval with null organization check
            Map<String, Object> analytics = null;
            try {
                analytics = organizationDashboardAnalyticsService.getOrganizationAnalytics(organization);
            } catch (Exception e) {
                logger.error("Analytics service failed for organization {}: {}", organization.getName(), e.getMessage(), e);
                analytics = null; // Will trigger fallback
            }
            if (analytics != null) {
                // Add all analytics to model
                model.addAllAttributes(analytics);

                // Legacy compatibility - keep existing attribute names
                model.addAttribute("totalVouchers", analytics.get("totalVouchers"));

                // Calculate percentage changes for legacy compatibility
                Long totalVouchers = (Long) analytics.get("totalVouchers");
                Long approvedVouchers = (Long) analytics.get("approvedVouchers");
                if (totalVouchers != null && approvedVouchers != null && totalVouchers > 0) {
                    double voucherPercentageChange = ((double) approvedVouchers / totalVouchers) * 100;
                    model.addAttribute("voucherPercentageChange", String.format("%.2f", voucherPercentageChange));
                } else {
                    model.addAttribute("voucherPercentageChange", "0.00");
                }

            } else {
                logger.warn("Analytics returned null, using fallback values");
                // Fallback to legacy methods if analytics service fails
                Map<String, Object> voucherStats = voucherService.getVoucherStats();
                if (voucherStats != null) {
                    model.addAttribute("totalVouchers", voucherStats.get("totalVouchers"));
                    model.addAttribute("voucherPercentageChange", voucherStats.get("voucherPercentageChange"));
                } else {
                    model.addAttribute("totalVouchers", 0);
                    model.addAttribute("voucherPercentageChange", "0.00");
                }
            }

            model.addAttribute("organization", organization);
            model.addAttribute("organizationAdmin", organizationAdmin);
            model.addAttribute("pageTitle", "Dashboard");

            if (country != null) {
                model.addAttribute("country", country);
            } else {
                logger.warn("Country 'Zambia' not found in settings");
            }

            logger.debug("Successfully loaded organization portal dashboard");
            return "index";

        } catch (Exception ex) {
            logger.error("Error loading organization portal dashboard: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Failed to load dashboard. Please try again later.");
            return "error/500";
        }
    }

    @GetMapping("/login")
    public String viewLoginPage(Model model) {
        try {
            logger.debug("Loading login page");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
                return "security/login";
            }
            return "redirect:/organizations/dashboard";
        } catch (Exception ex) {
            logger.error("Error loading login page: {}", ex.getMessage(), ex);
            return "security/login";
        }
    }

    @GetMapping("/sign-up")
    public String viewSignUpPage(Model model) {
        try {
            logger.debug("Loading sign-up page");
            return "register/signup_home";
        } catch (Exception ex) {
            logger.error("Error loading sign-up page: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Failed to load sign-up page. Please try again later.");
            return "error/500";
        }
    }

    @GetMapping("/api/dashboard/clients")
    @ResponseBody
    public Map<String, Object> getDashboardClientData(HttpServletRequest request) {
        try {
            OrganizationAdmin organizationAdmin = organizationAdminService.getAuthenticatedOrganizationAdmin(request);
            if (organizationAdmin == null) {
                return Map.of("error", "Authentication required");
            }

            Organization organization = organizationService.getOrganizationByOrganizationAdmin(organizationAdmin);
            if (organization == null) {
                return Map.of("error", "Organization not found");
            }

            Map<String, Object> analytics = organizationDashboardAnalyticsService.getOrganizationAnalytics(organization);

            Map<String, Object> clientData = new HashMap<>();
            clientData.put("totalClients", analytics.get("totalClients"));
            clientData.put("activeClients", analytics.get("activeClients"));
            clientData.put("inactiveClients", analytics.get("inactiveClients"));

            return clientData;
        } catch (Exception ex) {
            logger.error("Error getting dashboard client data: {}", ex.getMessage(), ex);
            return Map.of("error", "Failed to load client data");
        }
    }

    @GetMapping("/api/dashboard/vouchers")
    @ResponseBody
    public Map<String, Object> getDashboardVoucherData(HttpServletRequest request) {
        try {
            OrganizationAdmin organizationAdmin = organizationAdminService.getAuthenticatedOrganizationAdmin(request);
            if (organizationAdmin == null) {
                return Map.of("error", "Authentication required");
            }

            Organization organization = organizationService.getOrganizationByOrganizationAdmin(organizationAdmin);
            if (organization == null) {
                return Map.of("error", "Organization not found");
            }

            Map<String, Object> analytics = organizationDashboardAnalyticsService.getOrganizationAnalytics(organization);

            Map<String, Object> voucherData = new HashMap<>();
            voucherData.put("totalVouchers", analytics.get("totalVouchers"));
            voucherData.put("approvedVouchers", analytics.get("approvedVouchers"));
            voucherData.put("pendingVouchers", analytics.get("pendingVouchers"));
            voucherData.put("voucherApprovalRate", analytics.get("voucherApprovalRate"));

            return voucherData;
        } catch (Exception ex) {
            logger.error("Error getting dashboard voucher data: {}", ex.getMessage(), ex);
            return Map.of("error", "Failed to load voucher data");
        }
    }
}
