package org.paykesho.backoffice.adminauth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AdminAuthController {
    @GetMapping({"/login","/admin/login"})
    public String login(){return "admin-login";}

    @GetMapping("/ping")
    @ResponseBody
    public String ping(){return "ok";}
    @GetMapping("/") public String dashboard(){return "admin-dashboard";}
}
