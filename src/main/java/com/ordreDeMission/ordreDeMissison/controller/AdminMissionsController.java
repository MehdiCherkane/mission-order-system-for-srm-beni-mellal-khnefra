package com.ordreDeMission.ordreDeMissison.controller;

import com.ordreDeMission.ordreDeMissison.model.Employee;
import com.ordreDeMission.ordreDeMissison.model.Mission;
import com.ordreDeMission.ordreDeMissison.service.EmployeeService;
import com.ordreDeMission.ordreDeMissison.service.MissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/missions")
public class AdminMissionsController {

    private static final List<String> STATUTS = List.of(
            "soumise", "en_attente_chef", "en_attente_directeur", "approuvee", "rejetee");

    private final MissionService missionService;
    private final EmployeeService employeeService;

    public AdminMissionsController(MissionService missionService, EmployeeService employeeService) {
        this.missionService = missionService;
        this.employeeService = employeeService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String statut,
                       @RequestParam(required = false) UUID requesterId,
                       @RequestParam(required = false) String role,
                       HttpSession session, Model model) {
        List<Mission> missions = missionService.findAll();

        if (statut != null && !statut.isBlank() && !"all".equals(statut)) {
            missions = missions.stream()
                    .filter(m -> statut.equals(m.getStatut()))
                    .toList();
        }
        if (requesterId != null) {
            missions = missions.stream()
                    .filter(m -> m.getRequester() != null && requesterId.equals(m.getRequester().getId()))
                    .toList();
        }
        if (role != null && !role.isBlank() && !"all".equals(role)) {
            missions = missions.stream()
                    .filter(m -> m.getRequester() != null && role.equals(m.getRequester().getRole()))
                    .toList();
        }

        model.addAttribute("user", session.getAttribute("user"));
        model.addAttribute("missions", missions);
        model.addAttribute("statuts", STATUTS);
        model.addAttribute("statut", statut != null ? statut : "all");
        model.addAttribute("requesterId", requesterId);
        model.addAttribute("role", role != null ? role : "all");
        model.addAttribute("requesters", employeeService.findAll());
        model.addAttribute("totalCount", missionService.findAll().size());
        return "admin/missions";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, HttpSession session, Model model) {
        Mission m = missionService.findById(id);
        if (m == null) return "redirect:/admin/missions";
        model.addAttribute("user", session.getAttribute("user"));
        model.addAttribute("mission", m);
        return "admin/mission-detail";
    }
}
