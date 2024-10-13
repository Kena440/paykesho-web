package org.paykesho.backoffice.user.controller;

import org.commons.entity.Role;
import org.commons.entity.User;
import org.commons.enums.FileType;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.files.dto.FileUploadResponse;
import org.paykesho.backoffice.files.service.FileUploadService;
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
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping("")
    public String listAllUsersByPage(Model model) {
        model.addAttribute("pageTitle", "List Merchants");
        model.addAttribute("userList", userService.listAll());
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
    @Transactional
    public String saveUser(User user, RedirectAttributes redirectAttributes, @RequestParam("image") MultipartFile multipartFile) throws PaykeshoError {
        try {
            if (!multipartFile.isEmpty()) {
                String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));

                if (!isValidImageType(multipartFile)) {
                    throw new PaykeshoError("Invalid image type uploaded");
                }

                user.setPhotos(originalFileName);
                FileUploadResponse uploadResponse = fileUploadService.uploadFile(multipartFile, FileType.photo, 1, "profile photo for " + user.getEmail());

                if (!uploadResponse.isStatus()) {
                    throw new PaykeshoError("File upload failed.");
                }

                user.setRemoteFileId(uploadResponse.getFileId());
                user.setPhotos(uploadResponse.getFilePath());
                user.setPhotoPath(uploadResponse.getFilePath());
                userService.save(user);

            } else {
                if (user.getPhotos() == null || user.getPhotos().isEmpty()) {
                    user.setPhotos(null);
                    user.setPhotoPath(null);
                }
                userService.save(user);
            }

            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "User Operation Successful: " + user.getEmail());

        } catch (PaykeshoError e) {
            redirectAttributes.addFlashAttribute(Constants.ERROR, "Error saving user: " + e.getMessage());
            return "redirect:/users";
        }

        return getRedirectURLtoAffectedUser(user);
    }

    private boolean isValidImageType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png"));
    }

    private String getRedirectURLtoAffectedUser(User user) throws PaykeshoError {
        if (user == null) {
            throw new PaykeshoError("no user provided");
        }
        String firstPartOfEmail = user.getEmail().split("@")[0];
        return Constants.REDIRECT_TO_USERS;
    }

    @GetMapping("/edit/{id}")
    public String editUser(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws PaykeshoError {
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
    public String deleteUser(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws PaykeshoError {
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
    public String updateUserEnabledStatus(@PathVariable(name = "id") Integer id, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes, Model model) throws PaykeshoError {

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
    public String viewUserDetails(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) throws PaykeshoError {

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
