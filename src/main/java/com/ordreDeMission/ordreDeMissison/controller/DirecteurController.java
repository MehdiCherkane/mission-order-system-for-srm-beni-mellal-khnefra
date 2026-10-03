package com.ordreDeMission.ordreDeMissison.controller;

import com.ordreDeMission.ordreDeMissison.model.Employee;
import com.ordreDeMission.ordreDeMissison.model.Mission;
import com.ordreDeMission.ordreDeMissison.service.MissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/directeur")
public class DirecteurController {

    private final MissionService missionService;

    public DirecteurController(MissionService missionService) {
        this.missionService = missionService;
    }

    @GetMapping({"/missions", "/pending"})
    public String missions(@RequestParam(defaultValue = "PENDING") String status,
                           HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("user");
        String normalized = status == null ? "PENDING" : status.toUpperCase();
        List<Mission> missions = missionService.findDirecteurMissions(user.getId(), normalized);
        MissionService.MissionCounts counts = missionService.getMissionCounts(user.getId(), 2);
        model.addAttribute("user", user);
        model.addAttribute("missions", missions);
        model.addAttribute("currentStatus", normalized);
        model.addAttribute("pendingCount", counts.pending());
        model.addAttribute("processedCount", counts.processed());
        model.addAttribute("totalCount", counts.total());
        return "directeur/pending";
    }

    @GetMapping("/approval/{id}")
    public String approvalForm(@PathVariable UUID id, HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("user");
        Mission mission = missionService.requireAssignedApprover(id, 2, user.getId());
        if (mission == null) return "redirect:/directeur/pending";

        boolean alreadyProcessed = missionService.isStepAlreadyProcessed(id, 2);

        model.addAttribute("user", user);
        model.addAttribute("mission", mission);
        model.addAttribute("alreadyProcessed", alreadyProcessed);
        return "directeur/approval";
    }

    @PostMapping("/approval/{id}")
    public String processApproval(@PathVariable UUID id, @RequestParam String action,
                                  @RequestParam(required = false) String commentaire,
                                  HttpSession session, Model model,
                                  RedirectAttributes redirectAttributes) {
        Employee user = (Employee) session.getAttribute("user");
        Mission mission = missionService.requireAssignedApprover(id, 2, user.getId());
        if (mission == null) return "redirect:/directeur/pending";

        if ("rejete".equals(action) && (commentaire == null || commentaire.trim().isEmpty())) {
            model.addAttribute("user", user);
            model.addAttribute("mission", mission);
            model.addAttribute("rejectionError", "Veuillez fournir un motif de rejet.");
            return "directeur/approval";
        }

        missionService.approveByDirecteur(id, action, commentaire);
        redirectAttributes.addFlashAttribute("toast",
                "rejete".equals(action) ? "Mission rejet\u00e9e." : "Mission approuv\u00e9e.");
        return "redirect:/directeur/pending";
    }
}
