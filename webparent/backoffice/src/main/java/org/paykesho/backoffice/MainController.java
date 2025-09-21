package org.paykesho.backoffice;

import org.commons.entity.settings.Country;
import org.paykesho.backoffice.dashboard.service.DashboardAnalyticsService;
import org.paykesho.backoffice.organizations.service.OrganizationService;
import org.paykesho.backoffice.settings.service.SettingService;
import org.paykesho.backoffice.transaction.service.TransactionsService;
import org.paykesho.backoffice.voucher.service.VoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class MainController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);
    @Autowired
    private SettingService settingService;
    @Autowired
    private OrganizationService facilityService;
    @Autowired
    private TransactionsService transactionsService;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private DashboardAnalyticsService dashboardAnalyticsService;

    @GetMapping("")
    public String viewHomePage(Model model, HttpServletRequest request) {
        try {
            LOGGER.debug("Loading backoffice dashboard for request: {}", request.getRequestURI());

            // Get country settings
            Country country = settingService.getCountryByName("Zambia");
            if (country != null) {
                model.addAttribute("country", country);
            } else {
                LOGGER.warn("Country 'Zambia' not found in settings");
            }

            // Get comprehensive system-wide analytics
            Map<String, Object> analytics = dashboardAnalyticsService.getSystemWideAnalytics();
            if (analytics != null) {
                // Add all analytics to model
                model.addAllAttributes(analytics);

                // Legacy compatibility - keep existing attribute names
                model.addAttribute("totalTransactions", analytics.get("totalTransactions"));
                model.addAttribute("totalVouchers", analytics.get("totalVouchers"));
                model.addAttribute("facilityCount", analytics.get("totalOrganizations"));

                // Calculate percentage changes for legacy compatibility
                Long totalTransactions = (Long) analytics.get("totalTransactions");
                Long transactionsThisWeek = (Long) analytics.get("transactionsThisWeek");
                if (totalTransactions != null && transactionsThisWeek != null && totalTransactions > 0) {
                    double txnPercentageChange = ((double) transactionsThisWeek / (totalTransactions - transactionsThisWeek)) * 100;
                    model.addAttribute("txnPercentageChange", String.format("%.2f", txnPercentageChange));
                } else {
                    model.addAttribute("txnPercentageChange", "0.00");
                }

                Long totalVouchers = (Long) analytics.get("totalVouchers");
                Long approvedVouchers = (Long) analytics.get("approvedVouchers");
                if (totalVouchers != null && approvedVouchers != null && totalVouchers > 0) {
                    double voucherPercentageChange = ((double) approvedVouchers / totalVouchers) * 100;
                    model.addAttribute("voucherPercentageChange", String.format("%.2f", voucherPercentageChange));
                } else {
                    model.addAttribute("voucherPercentageChange", "0.00");
                }

            } else {
                LOGGER.warn("Analytics returned null, using fallback values");
                // Fallback to legacy methods if analytics service fails
                Map<String, Object> txnStats = transactionsService.getTransactionStats();
                if (txnStats != null) {
                    model.addAttribute("totalTransactions", txnStats.get("totalTransactions"));
                    model.addAttribute("txnPercentageChange", txnStats.get("txnPercentageChange"));
                } else {
                    model.addAttribute("totalTransactions", 0);
                    model.addAttribute("txnPercentageChange", "0.00");
                }

                Map<String, Object> voucherStats = voucherService.getVoucherStats();
                if (voucherStats != null) {
                    model.addAttribute("totalVouchers", voucherStats.get("totalVouchers"));
                    model.addAttribute("voucherPercentageChange", voucherStats.get("voucherPercentageChange"));
                } else {
                    model.addAttribute("totalVouchers", 0);
                    model.addAttribute("voucherPercentageChange", "0.00");
                }

                try {
                    model.addAttribute("facilityCount", facilityService.countInstitutions());
                } catch (Exception ex) {
                    LOGGER.warn("Failed to get facility count: {}", ex.getMessage());
                    model.addAttribute("facilityCount", 0);
                }
            }

            model.addAttribute("pageTitle", "Dashboard");

            LOGGER.debug("Successfully loaded backoffice dashboard with enhanced analytics");
            return "index";

        } catch (Exception ex) {
            LOGGER.error("Error loading backoffice dashboard: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Failed to load dashboard. Please try again later.");
            return "error/500";
        }
    }

    @GetMapping("/login")
    public String viewLoginPage() {
        try {
            LOGGER.debug("Loading login page");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
                return "security/login";
            }

            return "redirect:/";
        } catch (Exception ex) {
            LOGGER.error("Error loading login page: {}", ex.getMessage(), ex);
            return "security/login";
        }
    }


}
