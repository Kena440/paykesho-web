package org.paykesho.backoffice.user.controller;

import org.commons.entity.User;
import org.paykesho.backoffice.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/rest")
public class UserRestController {

    @Autowired
    private UserService userService;

    @PostMapping("/check-email")
    public String checkDuplicateEmail(String uuid, String email) {
        return userService.isEmailUniqueByUuid(uuid, email) ? "OK" : "Duplicated";
    }

    @GetMapping("/enabled")
    public List<User> getEnabledUsers() {
        return userService.getAllEnabledUsers();
    }
}
