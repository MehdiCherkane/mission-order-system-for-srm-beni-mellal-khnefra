package com.ordreDeMission.ordreDeMissison.controller;

import com.ordreDeMission.ordreDeMissison.model.Employee;
import com.ordreDeMission.ordreDeMissison.service.EmployeeService;
import com.ordreDeMission.ordreDeMissison.service.ServiceApproverService;
import com.ordreDeMission.ordreDeMissison.service.SystemConfigService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    public record ServiceRoutingRow(String serviceName, String chefMatricule) {}

    private final SystemConfigService configService;
    private final EmployeeService employeeService;
    private final ServiceApproverService serviceApproverService;

    public AdminSettingsController(SystemConfigService configService, EmployeeService employeeService,
                                   ServiceApproverService serviceApproverService) {
        this.configService = configService;
        this.employeeService = employeeService;
        this.serviceApproverService = serviceApproverService;
    }

    @GetMapping
    public String form(HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("user");

        String chefMatricule = configService.get(SystemConfigService.KEY_DEFAULT_CHEF);
        String dirMatricule = configService.get(SystemConfigService.KEY_DEFAULT_DIRECTEUR);

        List<Employee> allEmployees = employeeService.findAll();
        List<Employee> chefs = allEmployees.stream()
                .filter(e -> "CHEF_HIERARCHIQUE".equals(e.getRole()))
                .toList();
        List<Employee> directeurs = allEmployees.stream()
                .filter(e -> "DIRECTEUR".equals(e.getRole()))
                .toList();
        Map<String, com.ordreDeMission.ordreDeMissison.model.ServiceApprover> mappings =
                serviceApproverService.mappingsByServiceKey();
        List<ServiceRoutingRow> routingRows = serviceApproverService.findAllServiceNames().stream()
                .map(service -> {
                    var mapping = mappings.get(ServiceApproverService.serviceKey(service));
                    String mappedChef = mapping != null && mapping.getChef() != null
                            && "CHEF_HIERARCHIQUE".equals(mapping.getChef().getRole())
                            ? mapping.getChef().getMatricule() : "";
                    return new ServiceRoutingRow(service, mappedChef);
                })
                .toList();

        boolean validDefaultChef = chefMatricule != null && chefs.stream()
                .anyMatch(chef -> chefMatricule.equals(chef.getMatricule()));
        List<String> routingWarnings = new ArrayList<>();
        if (chefs.isEmpty()) {
            routingWarnings.add("Aucun employé avec le rôle CHEF_HIERARCHIQUE n'est disponible à l'affectation.");
        }
        if (!validDefaultChef) {
            routingRows.stream()
                    .filter(row -> row.chefMatricule() == null || row.chefMatricule().isBlank())
                    .forEach(row -> routingWarnings.add("Le service « " + row.serviceName()
                            + " » n'a aucun chef affecté ni chef de secours."));
        }

        model.addAttribute("user", user);
        model.addAttribute("chefs", chefs);
        model.addAttribute("directeurs", directeurs);
        model.addAttribute("defaultChef", chefMatricule == null ? "" : chefMatricule);
        model.addAttribute("defaultDirecteur", dirMatricule == null ? "" : dirMatricule);
        model.addAttribute("serviceRoutingRows", routingRows);
        model.addAttribute("routingWarnings", routingWarnings);
        return "admin/settings";
    }

    @PostMapping
    public String saveRouting(@RequestParam(required = false) String defaultChef,
                              @RequestParam(required = false) String defaultDirecteur,
                              @RequestParam(required = false, name = "serviceName") List<String> serviceNames,
                              @RequestParam(required = false, name = "serviceChef") List<String> serviceChefs,
                              @RequestParam(required = false) String newServiceName,
                              @RequestParam(required = false) String newServiceChef,
                              RedirectAttributes ra) {
        if (defaultDirecteur == null || defaultDirecteur.isBlank()) {
            ra.addFlashAttribute("settingsError", "Le directeur provincial par défaut est requis.");
            return "redirect:/admin/settings";
        }

        String normalizedDefaultChef = defaultChef == null ? "" : defaultChef.trim();
        if (!normalizedDefaultChef.isEmpty()) {
            Employee chef = employeeService.findByMatricule(normalizedDefaultChef);
            if (chef == null || !"CHEF_HIERARCHIQUE".equals(chef.getRole())) {
                ra.addFlashAttribute("settingsError", "Le chef de secours doit avoir le rôle CHEF_HIERARCHIQUE.");
                return "redirect:/admin/settings";
            }
        }

        Employee dir = employeeService.findByMatricule(defaultDirecteur.trim());
        if (dir == null || !"DIRECTEUR".equals(dir.getRole())) {
            ra.addFlashAttribute("settingsError", "Le directeur doit avoir le rôle DIRECTEUR.");
            return "redirect:/admin/settings";
        }

        List<ServiceApproverService.MappingInput> mappings = new ArrayList<>();
        int count = serviceNames == null ? 0 : serviceNames.size();
        for (int i = 0; i < count; i++) {
            String chef = serviceChefs != null && i < serviceChefs.size() ? serviceChefs.get(i) : null;
            mappings.add(new ServiceApproverService.MappingInput(serviceNames.get(i), chef));
        }
        if (newServiceName != null && !newServiceName.isBlank()) {
            mappings.add(new ServiceApproverService.MappingInput(newServiceName, newServiceChef));
        }

        try {
            serviceApproverService.saveMappings(mappings);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("settingsError", ex.getMessage());
            return "redirect:/admin/settings";
        }

        configService.set(SystemConfigService.KEY_DEFAULT_CHEF, normalizedDefaultChef,
                "Chef hiérarchique par défaut, utilisé si aucun chef n'est affecté au service");
        configService.set(SystemConfigService.KEY_DEFAULT_DIRECTEUR, defaultDirecteur.trim(),
                "Directeur provincial par défaut");
        ra.addFlashAttribute("settingsSuccess", "Paramètres et routage par service enregistrés.");
        return "redirect:/admin/settings";
    }
}
