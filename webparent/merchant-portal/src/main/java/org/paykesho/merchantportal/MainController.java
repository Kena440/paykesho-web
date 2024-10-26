package org.paykesho.merchantportal;

import org.commons.entity.merchant.Merchant;
import org.commons.entity.merchant.MerchantAdmin;
import org.commons.entity.settings.Country;
import org.paykesho.merchantportal.merchantadmins.service.MerchantAdminService;
import org.paykesho.merchantportal.merchants.service.MerchantCodeService;
import org.paykesho.merchantportal.merchants.service.MerchantService;
import org.paykesho.merchantportal.settings.service.SettingService;
import org.paykesho.merchantportal.transactions.service.TransactionsService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private MerchantAdminService merchantAdminService;
    @Autowired
    private MerchantService merchantService;
    @Autowired
    private TransactionsService transactionsService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private MerchantCodeService merchantCodeService;

    @GetMapping("")
    public String viewHomePage(Model model, HttpServletRequest request) {
        MerchantAdmin merchantAdmin = merchantAdminService.getAuthenticatedMerchantAdmin(request);
        Merchant merchant = merchantService.getMerchantByContactPerson(merchantAdmin);
        Country country = settingService.getCountryByName("Zambia");
        Map<String, Object> txnStats = transactionsService.getTransactionStats();
        model.addAttribute("totalTransactions", txnStats.get("totalTransactions"));
        model.addAttribute("txnPercentageChange", txnStats.get("txnPercentageChange"));
        model.addAttribute("merchantAdmin", merchantAdmin);
        model.addAttribute("merchant", merchant);
        model.addAttribute("qrCodeContent", "/generateQRCode?qrContent=" + merchant.getCode());

        if (country != null) model.addAttribute("country", country);

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

    @GetMapping("/generateQRCode")
    public void generateQRCode(String qrContent, HttpServletResponse response) throws IOException {
        response.setContentType("image/png");
        byte[] qrCode = merchantCodeService.generateQRCode(qrContent, 500, 500);
        OutputStream outputStream = response.getOutputStream();
        outputStream.write(qrCode);
    }
}
