package org.paykesho.merchantportal.merchantadmins.controller;

import org.commons.security.PasswordChange;
import org.commons.entity.merchant.MerchantAdmin;
import org.paykesho.merchantportal.merchantadmins.repository.MerchantAdminRepository;
import org.paykesho.merchantportal.config.Utility;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * "My account" for the signed-in user: shows their details and lets them change
 * their own password (the current password is required). The account is always
 * the one in the session; nothing in the request can point at another account.
 *
 * Replaces the old account form, which saved whatever account id the form sent.
 */
@Controller
public class MyAccountController {

    private static final String PAGE = "/account";

    private final MerchantAdminRepository repository;
    private final PasswordEncoder passwordEncoder;

    public MyAccountController(MerchantAdminRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping(PAGE)
    public String view(HttpServletRequest request, Model model) {
        MerchantAdmin account = current(request);
        if (account == null) return "redirect:/login";
        model.addAttribute("pageTitle", "My Account");
        model.addAttribute("displayName", account.getName());
        model.addAttribute("email", account.getEmail());
        model.addAttribute("canChangePassword", account.getPassword() != null && !account.getPassword().isEmpty());
        model.addAttribute("passwordAction", PAGE + "/password");
        return "account/my_account";
    }

    @PostMapping(PAGE + "/password")
    public String changePassword(HttpServletRequest request,
                                 @RequestParam(value = "currentPassword", required = false) String currentPassword,
                                 @RequestParam(value = "newPassword", required = false) String newPassword,
                                 @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        MerchantAdmin account = current(request);
        if (account == null) return "redirect:/login";
        String problem = PasswordChange.problem(account.getPassword(), currentPassword, newPassword, confirmPassword,
                passwordEncoder::matches);
        if (problem != null) {
            redirectAttributes.addFlashAttribute("error", problem);
        } else {
            account.setPassword(passwordEncoder.encode(newPassword));
            repository.save(account);
            redirectAttributes.addFlashAttribute("message", "Your password has been changed.");
        }
        return "redirect:" + PAGE;
    }

    private MerchantAdmin current(HttpServletRequest request) {
        String email = Utility.getEKeshofAuthenticatedMerchantAdmin(request);
        return email == null ? null : repository.findByEmail(email);
    }
}
