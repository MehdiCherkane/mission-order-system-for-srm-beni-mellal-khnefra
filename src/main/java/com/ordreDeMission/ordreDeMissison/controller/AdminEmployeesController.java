package com.ordreDeMission.ordreDeMissison.controller;

import com.ordreDeMission.ordreDeMissison.model.Employee;
import com.ordreDeMission.ordreDeMissison.service.EmployeeAdminService;
import com.ordreDeMission.ordreDeMissison.service.EmployeeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin/employees")
public class AdminEmployeesController {

    private static final List<String> ALLOWED_ROLES = List.of(
            "EMPLOYEE", "CHEF_HIERARCHIQUE", "DIRECTEUR", "ADMIN");

    private final EmployeeService employeeService;
    private final EmployeeAdminService adminService;

    public AdminEmployeesController(EmployeeService employeeService, EmployeeAdminService adminService) {
        this.employeeService = employeeService;
        this.adminService = adminService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String role,
                       HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("user");
        List<Employee> all = employeeService.findAll();
        if (q != null && !q.isBlank()) {
            String qq = q.toLowerCase();
            all = all.stream()
                    .filter(e -> e.getNom().toLowerCase().contains(qq)
                            || e.getPrenom().toLowerCase().contains(qq)
                            || e.getMatricule().toLowerCase().contains(qq))
                    .toList();
        }
        if (role != null && !role.isBlank() && !"all".equals(role)) {
            all = all.stream().filter(e -> role.equals(e.getRole())).toList();
        }
        model.addAttribute("user", user);
        model.addAttribute("employees", all);
        model.addAttribute("q", q != null ? q : "");
        model.addAttribute("role", role != null ? role : "all");
        model.addAttribute("roles", ALLOWED_ROLES);
        model.addAttribute("editEmployee", new Employee());
        return "admin/employees";
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        model.addAttribute("user", session.getAttribute("user"));
        model.addAttribute("editEmployee", new Employee());
        model.addAttribute("roles", ALLOWED_ROLES);
        model.addAttribute("formAction", "/admin/employees/add");
        model.addAttribute("isNew", true);
        return "admin/employee-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, HttpSession session, Model model,
                           RedirectAttributes ra) {
        Employee e = employeeService.findById(id);
        if (e == null) {
            ra.addFlashAttribute("employeeError", "Employé introuvable.");
            return "redirect:/admin/employees";
        }
        model.addAttribute("user", session.getAttribute("user"));
        model.addAttribute("editEmployee", e);
        model.addAttribute("roles", ALLOWED_ROLES);
        model.addAttribute("formAction", "/admin/employees/" + id + "/update");
        model.addAttribute("isNew", false);
        return "admin/employee-form";
    }

    @PostMapping("/add")
    public String add(@RequestParam String matricule,
                      @RequestParam String nom,
                      @RequestParam String prenom,
                      @RequestParam(required = false) String fonction,
                      @RequestParam(required = false) String service,
                      @RequestParam(required = false) String direction,
                      @RequestParam String role,
                      @RequestParam(required = false) String email,
                      @RequestParam String motDePasse,
                      RedirectAttributes ra) {
        Map<String, String> errors = validate(null, matricule, nom, prenom, role, motDePasse);
        if (!errors.isEmpty()) {
            ra.addFlashAttribute("errors", errors);
            ra.addFlashAttribute("submittedMatricule", matricule);
            ra.addFlashAttribute("submittedNom", nom);
            ra.addFlashAttribute("submittedPrenom", prenom);
            ra.addFlashAttribute("submittedFonction", fonction);
            ra.addFlashAttribute("submittedService", service);
            ra.addFlashAttribute("submittedDirection", direction);
            ra.addFlashAttribute("submittedRole", role);
            ra.addFlashAttribute("submittedEmail", email);
            return "redirect:/admin/employees/new";
        }
        if (adminService.matriculeExists(matricule.trim())) {
            ra.addFlashAttribute("employeeError", "Ce matricule est déjà utilisé.");
            return "redirect:/admin/employees/new";
        }
        try {
            Employee e = new Employee(
                    matricule.trim(), nom.trim(), prenom.trim(),
                    safe(fonction), safe(service), safe(direction),
                    role.trim(), safe(email), "placeholder");
            adminService.create(e, motDePasse);
            ra.addFlashAttribute("employeeSuccess", "Employé créé.");
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("employeeError", "Conflit lors de la création.");
        }
        return "redirect:/admin/employees";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable UUID id,
                         @RequestParam String matricule,
                         @RequestParam String nom,
                         @RequestParam String prenom,
                         @RequestParam(required = false) String fonction,
                         @RequestParam(required = false) String service,
                         @RequestParam(required = false) String direction,
                         @RequestParam String role,
                         @RequestParam(required = false) String email,
                         HttpSession session,
                         RedirectAttributes ra) {
        Employee current = (Employee) session.getAttribute("user");
        if (current != null && current.getId().equals(id)) {
            ra.addFlashAttribute("employeeError", "Vous ne pouvez pas modifier votre propre compte ici.");
            return "redirect:/admin/employees";
        }
        Employee e = employeeService.findById(id);
        if (e == null) {
            ra.addFlashAttribute("employeeError", "Employé introuvable.");
            return "redirect:/admin/employees";
        }
        Map<String, String> errors = validate(id, matricule, nom, prenom, role, null);
        if (!errors.isEmpty()) {
            ra.addFlashAttribute("errors", errors);
            return "redirect:/admin/employees/" + id + "/edit";
        }
        if (adminService.matriculeExistsExcept(matricule.trim(), id)) {
            ra.addFlashAttribute("employeeError", "Ce matricule est déjà utilisé par un autre employé.");
            return "redirect:/admin/employees/" + id + "/edit";
        }
        e.setMatricule(matricule.trim());
        e.setNom(nom.trim());
        e.setPrenom(prenom.trim());
        e.setFonction(safe(fonction));
        e.setService(safe(service));
        e.setDirection(safe(direction));
        e.setRole(role.trim());
        e.setEmail(safe(email));
        adminService.update(e);
        ra.addFlashAttribute("employeeSuccess", "Employé mis à jour.");
        return "redirect:/admin/employees";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable UUID id, @RequestParam String motDePasse,
                                RedirectAttributes ra) {
        Employee current = employeeService.findById(id);
        if (current == null) {
            ra.addFlashAttribute("employeeError", "Employé introuvable.");
            return "redirect:/admin/employees";
        }
        if (motDePasse == null || motDePasse.length() < 6) {
            ra.addFlashAttribute("employeeError", "Le mot de passe doit faire au moins 6 caractères.");
            return "redirect:/admin/employees/" + id + "/edit";
        }
        adminService.resetPassword(id, motDePasse);
        ra.addFlashAttribute("employeeSuccess", "Mot de passe réinitialisé.");
        return "redirect:/admin/employees/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, HttpSession session, RedirectAttributes ra) {
        Employee current = (Employee) session.getAttribute("user");
        if (current != null && current.getId().equals(id)) {
            ra.addFlashAttribute("employeeError", "Vous ne pouvez pas supprimer votre propre compte.");
            return "redirect:/admin/employees";
        }
        EmployeeAdminService.DeleteResult res = adminService.delete(id);
        if (res == EmployeeAdminService.DeleteResult.ASSIGNED_TO_SERVICE) {
            ra.addFlashAttribute("employeeError",
                    "Cet employ\u00e9 est encore affect\u00e9 comme chef d'un service. R\u00e9affectez ce service avant de le supprimer.");
            return "redirect:/admin/employees";
        }
        switch (res) {
            case OK -> ra.addFlashAttribute("employeeSuccess", "Employé supprimé.");
            case NOT_FOUND -> ra.addFlashAttribute("employeeError", "Employé introuvable.");
            case HAS_HISTORY -> ra.addFlashAttribute("employeeError",
                    "Cet employé a un historique de missions et ne peut pas être supprimé.");
        }
        return "redirect:/admin/employees";
    }

    private Map<String, String> validate(UUID id, String matricule, String nom, String prenom,
                                          String role, String motDePasse) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (matricule == null || matricule.trim().isEmpty()) {
            errors.put("matricule", "Matricule requis.");
        } else if (matricule.trim().length() > 50) {
            errors.put("matricule", "Matricule trop long (50 max).");
        }
        if (nom == null || nom.trim().isEmpty()) errors.put("nom", "Nom requis.");
        if (prenom == null || prenom.trim().isEmpty()) errors.put("prenom", "Prénom requis.");
        if (role == null || !ALLOWED_ROLES.contains(role)) errors.put("role", "Rôle invalide.");
        if (motDePasse != null && motDePasse.length() < 6) {
            errors.put("motDePasse", "Le mot de passe doit faire au moins 6 caractères.");
        }
        return errors;
    }

    private String safe(String s) { return s == null ? null : s.trim(); }
}
