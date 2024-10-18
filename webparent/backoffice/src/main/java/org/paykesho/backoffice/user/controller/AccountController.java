package org.paykesho.backoffice.user.controller;

import org.commons.entity.User;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.security.MembershipMgtUserDetails;
import org.paykesho.backoffice.user.enums.Constants;
import org.paykesho.backoffice.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private UserService userService;

    @GetMapping("")
    public String viewDetails(@AuthenticationPrincipal MembershipMgtUserDetails loggedInUser, Model model) throws PaykeshoError {
        String email = loggedInUser.getUsername();
        if (email == null) {
            throw new PaykeshoError("Null parameter email");
        }
        User user = userService.getUserByEmail(email);
        String pageTitle = "My Account";

        model.addAttribute("user", user);
        model.addAttribute("pageTitle", pageTitle);

        return "users/account_form";
    }

    @PostMapping("/update")
    public String saveDetails(User user,
                              RedirectAttributes redirectAttributes,
                              @AuthenticationPrincipal MembershipMgtUserDetails loggedUser,
                              @RequestParam("image") MultipartFile multipartFile) throws IOException, PaykeshoError {
        loggedUser.setFirstName(user.getFirstName());
        loggedUser.setLastName(user.getLastName());

        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Update successful: " + user.getEmail());
        return "redirect:/account";
    }

}
