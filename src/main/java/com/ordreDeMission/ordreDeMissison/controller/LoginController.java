package com.ordreDeMission.ordreDeMissison.controller;

import com.ordreDeMission.ordreDeMissison.model.Employee;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/")
    public String home(HttpSession session) {
        Employee user = (Employee) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        return switch (user.getRole()) {
            case "CHEF_HIERARCHIQUE" -> "redirect:/chef/pending";
            case "DIRECTEUR" -> "redirect:/directeur/pending";
            case "ADMIN" -> "redirect:/admin/dashboard";
            default -> "redirect:/employee/dashboard";
        };
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        return "login";
    }
}
