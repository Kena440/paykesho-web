package org.chaz.backoffice.user.controller;

import org.chaz.backoffice.security.MembershipMgtUserDetails;
import org.chaz.backoffice.user.enums.Constants;
import org.chaz.backoffice.user.service.UserService;
import org.chaz.backoffice.utils.AmazonS3Util;
import org.commons.entity.User;
import org.commons.utils.AssessmentToolError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Objects;

@Controller
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private UserService userService;

    @GetMapping("")
    public String viewDetails(@AuthenticationPrincipal MembershipMgtUserDetails loggedInUser, Model model) throws AssessmentToolError {
        String email = loggedInUser.getUsername();
        if (email == null) {
            throw new AssessmentToolError("Null parameter email");
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
                              @RequestParam("image") MultipartFile multipartFile) throws IOException, AssessmentToolError {
        if (!multipartFile.isEmpty()) {
            String fileName = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
            user.setPhotos(fileName);
            User savedUser = userService.updateAccount(user);
            String uploadDir = "user_photos/" + savedUser.getId();

            if (!fileName.isEmpty()) {
                AmazonS3Util.removeFolder(uploadDir);
                AmazonS3Util.uploadFile(uploadDir, fileName, multipartFile.getInputStream());
            }
        } else {
            if (user.getPhotos().isEmpty()) {
                user.setPhotos(null);
            }
            userService.updateAccount(user);
        }

        loggedUser.setFirstName(user.getFirstName());
        loggedUser.setLastName(user.getLastName());

        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Update successful: " + user.getEmail());
        return "redirect:/account";
    }

}
