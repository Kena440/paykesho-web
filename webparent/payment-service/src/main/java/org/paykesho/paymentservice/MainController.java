package org.paykesho.paymentservice;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class MainController {

    @GetMapping("")
    public String viewHomePage(Model model, HttpServletRequest request) {
        return "test";
    }


}
