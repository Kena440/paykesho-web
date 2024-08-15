package org.paykesho.backoffice.user.controller;

import org.commons.entity.Role;
import org.commons.entity.User;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.paging.PagingAndSortingHelper;
import org.paykesho.backoffice.paging.PagingAndSortingParam;
import org.paykesho.backoffice.user.enums.Constants;
import org.paykesho.backoffice.user.fileHandlers.UserCSVExporter;
import org.paykesho.backoffice.user.fileHandlers.UserExcelExporter;
import org.paykesho.backoffice.user.fileHandlers.UserPDFExporter;
import org.paykesho.backoffice.user.service.UserService;
import org.paykesho.backoffice.utils.AmazonS3Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("")
    public String listFirstPage() {
        return Constants.REDIRECT_TO_USERS_WITHOUT_KEYWORD;
    }

    @GetMapping("/page/{pageNum}")
    public String listAllUsersByPage(
            @PagingAndSortingParam(listName = "userList", moduleURL = "/users") PagingAndSortingHelper helper,
            @PathVariable("pageNum") int pageNum,
            Model model) throws PaykeshoError {
        userService.listByPage(pageNum, helper);
        model.addAttribute("pageTitle", "User List");

        return "users/users";
    }


    @GetMapping("/new")
    public String newUser(Model model) {
        String pageTitle = "New User";
        User user = new User();
        List<Role> roleList = userService.listRoles();

        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setFailedAttempt(0);

        model.addAttribute("user", user);
        model.addAttribute("roleList", roleList);
        model.addAttribute("pageTitle", pageTitle);
        return "users/user_form";
    }

    @PostMapping("/save")
    public String saveUser(User user,
                           RedirectAttributes redirectAttributes,
                           @RequestParam("image") MultipartFile multipartFile) throws IOException, PaykeshoError {
        if (!multipartFile.isEmpty()) {
            String fileName = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
            user.setPhotos(fileName);
            User savedUser = userService.save(user);
            String uploadDir = "user_photos/" + savedUser.getId();

            if (!fileName.isEmpty()) {
                AmazonS3Util.removeFolder(uploadDir);
                AmazonS3Util.uploadFile(uploadDir, fileName, multipartFile.getInputStream());
            }
        } else {
            if (user.getPhotos().isEmpty()) {
                user.setPhotos(null);
            }
            userService.save(user);
        }

        redirectAttributes.addFlashAttribute(Constants.MESSAGE, "User Operation Successful: " + user.getEmail());
        return getRedirectURLtoAffectedUser(user);
    }


    private String getRedirectURLtoAffectedUser(User user) throws PaykeshoError {
        if (user == null) {
            throw new PaykeshoError("no user provided");
        }
        String firstPartOfEmail = user.getEmail().split("@")[0];
        return Constants.REDIRECT_TO_USERS_KEYWORD + firstPartOfEmail;
    }

    @GetMapping("/edit/{id}")
    public String editUser(@PathVariable(name = "id") Integer id,
                           RedirectAttributes redirectAttributes,
                           Model model) throws PaykeshoError {
        if (id == null) {
            return Constants.REDIRECT_TO_USERS;
        }
        String pageTitle = "Edit User";

        try {
            User user = userService.getUserById(id);
            List<Role> roleList = userService.listRoles();

            model.addAttribute("user", user);
            model.addAttribute("roleList", roleList);
            model.addAttribute("pageTitle", pageTitle);

            return "users/user_form";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
            return Constants.REDIRECT_TO_USERS;
        }

    }

    @GetMapping("delete/{id}")
    public String deleteUser(@PathVariable(name = "id") Integer id,
                             RedirectAttributes redirectAttributes,
                             Model model) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("no parameter id");
        }

        try {
            userService.delete(id);

            String userPhotosDir = "user_photos/" + id;
            AmazonS3Util.removeFolder(userPhotosDir);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "User deleted successfully");
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
        }
        return Constants.REDIRECT_TO_USERS;
    }

    @GetMapping("/{id}/enabled/{status}")
    public String updateUserEnabledStatus(@PathVariable(name = "id") Integer id,
                                          @PathVariable(name = "status") boolean enabled,
                                          RedirectAttributes redirectAttributes,
                                          Model model) throws PaykeshoError {

        if (id == null) {
            throw new PaykeshoError("no parameter id");
        }

        try {
            User user = userService.getUserById(id);
            userService.updateUserStatus(id, enabled);
            String status = enabled ? "enabled" : "has been disabled";
            String operationMSG = user.getFirstName() + " has been " + status;
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
        }
        return Constants.REDIRECT_TO_USERS;

    }

    @GetMapping("/details/{id}")
    public String viewUserDetails(@PathVariable("id") Integer id,
                                  Model model,
                                  RedirectAttributes redirectAttributes) throws PaykeshoError {

        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "users/user_details_modal";

    }

    @GetMapping("/export/{format}")
    public void exportUsersByFormat(@PathVariable(name = "format") String format, HttpServletResponse response) throws PaykeshoError, IOException {
        switch (format) {
            case "csv":
                exportToCSV(response);
                break;
            case "excel":
                exportToExcel(response);
                break;
            case "pdf":
                exportToPDF(response);
                break;
            default:
                throw new PaykeshoError("no format provided");
        }
    }

    public void exportToCSV(HttpServletResponse response) throws IOException {
        List<User> userList = userService.listAll();
        UserCSVExporter exporter = new UserCSVExporter();
        exporter.export(userList, response);
    }

    public void exportToExcel(HttpServletResponse response) throws IOException {
        List<User> userList = userService.listAll();
        UserExcelExporter exporter = new UserExcelExporter();
        exporter.export(userList, response);
    }

    public void exportToPDF(HttpServletResponse response) throws IOException {
        List<User> userList = userService.listAll();
        UserPDFExporter exporter = new UserPDFExporter();
        exporter.export(userList, response);
    }

}
