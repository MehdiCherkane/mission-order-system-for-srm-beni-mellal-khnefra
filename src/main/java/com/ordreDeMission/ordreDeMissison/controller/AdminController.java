package com.ordreDeMission.ordreDeMissison.controller;

import com.ordreDeMission.ordreDeMissison.model.Employee;
import com.ordreDeMission.ordreDeMissison.model.Mission;
import com.ordreDeMission.ordreDeMissison.model.Vehicle;
import com.ordreDeMission.ordreDeMissison.service.EmployeeService;
import com.ordreDeMission.ordreDeMissison.service.MissionService;
import com.ordreDeMission.ordreDeMissison.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final VehicleService vehicleService;
    private final MissionService missionService;
    private final EmployeeService employeeService;

    public AdminController(VehicleService vehicleService, MissionService missionService,
                           EmployeeService employeeService) {
        this.vehicleService = vehicleService;
        this.missionService = missionService;
        this.employeeService = employeeService;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("user");
        List<Mission> allMissions = missionService.findAll();
        Map<String, Long> counts = allMissions.stream()
                .collect(java.util.stream.Collectors.groupingBy(Mission::getStatut,
                        java.util.stream.Collectors.counting()));
        model.addAttribute("user", user);
        model.addAttribute("totalMissions", allMissions.size());
        model.addAttribute("totalEmployees", employeeService.findAll().size());
        model.addAttribute("totalVehicles", vehicleService.findAll().size());
        model.addAttribute("counts", counts);
        return "admin/dashboard";
    }

    @GetMapping("/vehicles")
    public String vehicles(HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("user");
        model.addAttribute("user", user);
        model.addAttribute("vehicles", vehicleService.findAll());
        return "admin/vehicles";
    }

    @PostMapping("/vehicles/add")
    public String addVehicle(@RequestParam String matricule, @RequestParam String modele,
                             @RequestParam String serviceRattache,
                             RedirectAttributes redirectAttributes) {
        if (matricule == null || matricule.trim().isEmpty()
                || modele == null || modele.trim().isEmpty()
                || serviceRattache == null || serviceRattache.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("vehicleError", "Tous les champs sont obligatoires.");
            return "redirect:/admin/vehicles";
        }
        try {
            vehicleService.save(new Vehicle(matricule.trim(), modele.trim(), serviceRattache.trim()));
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("vehicleError", "Un véhicule avec ce matricule existe déjà.");
        }
        return "redirect:/admin/vehicles";
    }

    @PostMapping("/vehicles/delete/{id}")
    public String deleteVehicle(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        if (!missionService.findByVehicleId(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("vehicleError",
                    "Ce véhicule est rattaché à au moins une mission et ne peut pas être supprimé.");
            return "redirect:/admin/vehicles";
        }
        vehicleService.delete(id);
        return "redirect:/admin/vehicles";
    }
}