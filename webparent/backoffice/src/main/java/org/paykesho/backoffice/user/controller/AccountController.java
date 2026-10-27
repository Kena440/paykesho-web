package org.paykesho.backoffice.user.controller;

import org.commons.entity.User;
import org.commons.security.PasswordChange;
import org.paykesho.backoffice.security.MembershipMgtUserDetails;
import org.paykesho.backoffice.user.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * "My account" for the signed-in back-office user: their details, and changing
 * their own password (current password required). Always acts on the account in
 * the session. Staff details are managed under Users.
 */
@Controller
@RequestMapping("/account")
public class AccountController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("")
    public String viewDetails(@AuthenticationPrincipal MembershipMgtUserDetails loggedInUser, Model model) {
        User user = current(loggedInUser);
        if (user == null) return "redirect:/login";
        model.addAttribute("pageTitle", "My Account");
        model.addAttribute("displayName", user.getFullName());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("canChangePassword", user.getPassword() != null && !user.getPassword().isEmpty());
        model.addAttribute("passwordAction", "/account/password");
        return "account/my_account";
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal MembershipMgtUserDetails loggedInUser,
                                 @RequestParam(value = "currentPassword", required = false) String currentPassword,
                                 @RequestParam(value = "newPassword", required = false) String newPassword,
                                 @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        User user = current(loggedInUser);
        if (user == null) return "redirect:/login";
        String problem = PasswordChange.problem(user.getPassword(), currentPassword, newPassword, confirmPassword,
                passwordEncoder::matches);
        if (problem != null) {
            redirectAttributes.addFlashAttribute("error", problem);
        } else {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("message", "Your password has been changed.");
        }
        return "redirect:/account";
    }

    private User current(MembershipMgtUserDetails loggedInUser) {
        if (loggedInUser == null || loggedInUser.getUsername() == null) return null;
        return userRepository.getUserByEmail(loggedInUser.getUsername());
    }
}
