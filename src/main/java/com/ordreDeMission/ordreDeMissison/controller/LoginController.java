package com.ordreDeMission.ordreDeMissison.controller;

import com.ordreDeMission.ordreDeMissison.model.Employee;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

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

    @GetMapping("/access-denied")
    public String accessDenied(HttpSession session, Model model, HttpServletResponse response) {
        Employee user = (Employee) session.getAttribute("user");
        log.warn("Accès refusé pour l'utilisateur {}",
                user != null ? user.getMatricule() : "anonyme");
        model.addAttribute("user", user);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return "error/403";
    }
}
