package org.paykesho.backoffice.user.controller;

import org.paykesho.backoffice.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/rest")
public class UserRestController {

    @Autowired
    private UserService userService;

    @PostMapping("/check-email")
    public String checkDuplicateEmail(String uuid, String email) {
        return userService.isEmailUniqueByUuid(uuid, email) ? "OK" : "Duplicated";
    }
}
