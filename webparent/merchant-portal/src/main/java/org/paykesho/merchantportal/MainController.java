package org.paykesho.merchantportal;

import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.MerchantAdmin;
import org.commons.entity.settings.Country;
import org.paykesho.merchantportal.dashboard.service.MerchantDashboardAnalyticsService;
import org.paykesho.merchantportal.merchantadmins.service.MerchantAdminService;
import org.paykesho.merchantportal.merchants.service.MerchantCodeService;
import org.paykesho.merchantportal.merchants.service.MerchantService;
import org.paykesho.merchantportal.settings.service.SettingService;
import org.paykesho.merchantportal.transactions.service.TransactionsService;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@Controller
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    @Lazy
    private MerchantAdminService merchantAdminService;
    @Autowired
    private MerchantService merchantService;
    @Autowired
    private TransactionsService transactionsService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private MerchantCodeService merchantCodeService;
    @Autowired
    private MerchantDashboardAnalyticsService merchantDashboardAnalyticsService;

    @GetMapping("")
    public String viewHomePage(Model model, HttpServletRequest request) {
        try {
            logger.debug("Loading merchant portal dashboard for request: {}", request.getRequestURI());

            MerchantAdmin merchantAdmin = merchantAdminService.getAuthenticatedMerchantAdmin(request);
            if (merchantAdmin == null) {
                logger.warn("No authenticated merchant admin found for request");
                model.addAttribute("errorMessage", "Authentication required. Please log in.");
                return "security/login";
            }

            Merchant merchant = merchantService.getMerchantByContactPerson(merchantAdmin);
            Country country = settingService.getCountryByName("Zambia");

            // Get comprehensive merchant analytics
            Map<String, Object> analytics = merchantDashboardAnalyticsService.getMerchantAnalytics(merchant);
            if (analytics != null) {
                // Add all analytics to model
                model.addAllAttributes(analytics);

                // Legacy compatibility - keep existing attribute names
                model.addAttribute("totalTransactions", analytics.get("totalTransactions"));

                // Calculate percentage changes for legacy compatibility
                Long totalTransactions = (Long) analytics.get("totalTransactions");
                Long weeklyTransactions = (Long) analytics.get("weeklyTransactions");
                if (totalTransactions != null && weeklyTransactions != null && totalTransactions > 0) {
                    double txnPercentageChange = ((double) weeklyTransactions / (totalTransactions - weeklyTransactions)) * 100;
                    model.addAttribute("txnPercentageChange", String.format("%.2f", txnPercentageChange));
                } else {
                    model.addAttribute("txnPercentageChange", "0.00");
                }

            } else {
                logger.warn("Analytics returned null, using fallback values");
                // Fallback to legacy methods if analytics service fails
                Map<String, Object> txnStats = transactionsService.getTransactionStats();
                if (txnStats != null) {
                    model.addAttribute("totalTransactions", txnStats.get("totalTransactions"));
                    model.addAttribute("txnPercentageChange", txnStats.get("txnPercentageChange"));
                } else {
                    model.addAttribute("totalTransactions", 0);
                    model.addAttribute("txnPercentageChange", "0.00");
                }
            }

            model.addAttribute("merchantAdmin", merchantAdmin);
            model.addAttribute("merchant", merchant);

            // Add null check to prevent NullPointerException
            if (merchant != null) {
                model.addAttribute("qrCodeContent", "/generateQRCode?qrContent=" + merchant.getCode());
            } else {
                logger.warn("Merchant is null for merchant admin: {}", merchantAdmin.getEmail());
                model.addAttribute("qrCodeContent", "");
            }

            if (country != null) {
                model.addAttribute("country", country);
            } else {
                logger.warn("Country 'Zambia' not found in settings");
            }

            logger.debug("Successfully loaded merchant portal dashboard");
            return "index";

        } catch (Exception ex) {
            logger.error("Error loading merchant portal dashboard: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Failed to load dashboard. Please try again later.");
            return "error/500";
        }
    }

    @GetMapping("/login")
    public String viewLoginPage() {
        try {
            logger.debug("Loading login page");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
                return "security/login";
            }
            return "redirect:/";
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

    @GetMapping("/generateQRCode")
    public void generateQRCode(String qrContent, HttpServletResponse response) throws IOException {
        try {
            logger.debug("Generating QR code for content: {}", qrContent);

            if (qrContent == null || qrContent.trim().isEmpty()) {
                logger.warn("QR code content is null or empty");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "QR code content cannot be empty");
                return;
            }

            response.setContentType("image/png");
            byte[] qrCode = merchantCodeService.generateQRCode(qrContent, 500, 500);

            if (qrCode == null || qrCode.length == 0) {
                logger.error("Failed to generate QR code for content: {}", qrContent);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate QR code");
                return;
            }

            OutputStream outputStream = response.getOutputStream();
            outputStream.write(qrCode);
            outputStream.flush();

            logger.debug("Successfully generated QR code for content: {}", qrContent);

        } catch (IOException ex) {
            logger.error("IO error generating QR code: {}", ex.getMessage(), ex);
            throw ex; // Re-throw IOException as it's already declared in method signature
        } catch (Exception ex) {
            logger.error("Error generating QR code: {}", ex.getMessage(), ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate QR code");
        }
    }
}
