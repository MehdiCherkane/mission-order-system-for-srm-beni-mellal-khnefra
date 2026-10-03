package com.ordreDeMission.ordreDeMissison.controller;

import com.ordreDeMission.ordreDeMissison.model.Employee;
import com.ordreDeMission.ordreDeMissison.model.Mission;
import com.ordreDeMission.ordreDeMissison.service.EmployeeService;
import com.ordreDeMission.ordreDeMissison.service.LoginAttemptService;
import com.ordreDeMission.ordreDeMissison.service.MissionService;
import com.ordreDeMission.ordreDeMissison.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/employee")
public class EmployeeController {

    private static final DateTimeFormatter DATE_FLEX = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .toFormatter();

    private final MissionService missionService;
    private final EmployeeService employeeService;
    private final VehicleService vehicleService;
    private final LoginAttemptService loginAttemptService;

    public EmployeeController(MissionService missionService, EmployeeService employeeService, VehicleService vehicleService, LoginAttemptService loginAttemptService) {
        this.missionService = missionService;
        this.employeeService = employeeService;
        this.vehicleService = vehicleService;
        this.loginAttemptService = loginAttemptService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String filter, HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("user");

        List<Mission> missions = missionService.findByRequester(user.getId());

        if (filter != null && !filter.equals("all")) {
            missions = missions.stream()
                    .filter(m -> switch (filter) {
                        case "en_attente" -> m.getStatut().startsWith("en_attente");
                        case "approuvees" -> m.getStatut().equals("approuvee");
                        case "rejetees" -> m.getStatut().equals("rejetee");
                        default -> true;
                    })
                    .collect(Collectors.toList());
        }

        model.addAttribute("user", user);
        model.addAttribute("missions", missions);
        model.addAttribute("activeFilter", filter != null ? filter : "all");
        return "employee/dashboard";
    }

    @GetMapping("/mission/new")
    public String newMission(HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("user");
        model.addAttribute("user", user);
        model.addAttribute("mission", new Mission());
        model.addAttribute("vehicles", vehicleService.findAll());
        model.addAttribute("employees", employeeService.findAll());
        return "employee/mission-form";
    }

    @PostMapping("/mission/save")
    public String saveMission(@RequestParam String objet,
                              @RequestParam String destination,
                              @RequestParam String dateDepart,
                              @RequestParam String dateRetour,
                              @RequestParam String moyenTransport,
                              @RequestParam(required = false) UUID vehicleId,
                              @RequestParam(required = false) boolean combinee,
                              @RequestParam(required = false) String[] participants,
                              HttpSession session,
                                  Model model) {
        Employee user = (Employee) session.getAttribute("user");

        Map<String, String> errors = new LinkedHashMap<>();

        if (!loginAttemptService.isMissionCreationAllowed(user.getId())) {
            errors.put("routing", "Trop de demandes créées récemment. Veuillez réessayer dans une heure.");
        }

        // Validate objet
        if (objet == null || objet.trim().isEmpty()) {
            errors.put("objet", "L'objet de la mission est requis.");
        } else if (objet.trim().length() < 10) {
            errors.put("objet", "Veuillez décrire l'objet de la mission plus précisément (10 caractères minimum).");
        } else if (objet.length() > 500) {
            errors.put("objet", "L'objet ne peut pas dépasser 500 caractères.");
        }

        // Validate destination
        if (destination == null || destination.trim().isEmpty()) {
            errors.put("destination", "La destination est requise.");
        } else if (destination.trim().length() < 3) {
            errors.put("destination", "La destination doit contenir au moins 3 caractères.");
        }

        // Validate dates
        LocalDateTime departDate = null;
        LocalDateTime retourDate = null;

        if (dateDepart == null || dateDepart.trim().isEmpty()) {
            errors.put("dateDepart", "La date de départ est requise.");
        } else {
            try {
                departDate = LocalDateTime.parse(dateDepart, DATE_FLEX);
                if (departDate.isBefore(LocalDateTime.now())) {
                    errors.put("dateDepart", "La date de départ ne peut pas être dans le passé.");
                }
            } catch (DateTimeParseException e) {
                errors.put("dateDepart", "Format de date invalide.");
            }
        }

        if (dateRetour == null || dateRetour.trim().isEmpty()) {
            errors.put("dateRetour", "La date de retour est requise.");
        } else {
            try {
                retourDate = LocalDateTime.parse(dateRetour, DATE_FLEX);
            } catch (DateTimeParseException e) {
                errors.put("dateRetour", "Format de date invalide.");
            }
        }

        if (departDate != null && retourDate != null && !retourDate.isAfter(departDate)) {
            errors.put("dateRetour", "La date de retour doit être postérieure à la date de départ.");
        }

        // Validate moyen de transport
        if (moyenTransport == null || moyenTransport.trim().isEmpty()) {
            errors.put("moyenTransport", "Veuillez sélectionner un moyen de transport.");
        } else if (!moyenTransport.equals("vehicule_de_service") && !moyenTransport.equals("vehicule_personnel")) {
            errors.put("moyenTransport", "Moyen de transport invalide.");
        }

        // Validate participants when combined mission
        if (combinee && (participants == null || participants.length == 0)) {
            errors.put("participants", "Au moins un participant est requis pour une mission accompagnée.");
        }

        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("user", user);
            model.addAttribute("vehicles", vehicleService.findAll());
            model.addAttribute("employees", employeeService.findAll());
            model.addAttribute("submittedObjet", objet);
            model.addAttribute("submittedDestination", destination);
            model.addAttribute("submittedDateDepart", dateDepart);
            model.addAttribute("submittedDateRetour", dateRetour);
            model.addAttribute("submittedMoyenTransport", moyenTransport);
            model.addAttribute("submittedVehicleId", vehicleId);
            model.addAttribute("combinee", combinee);
            return "employee/mission-form";
        }

        Mission mission = new Mission();
        mission.setObjet(objet);
        mission.setDestination(destination);
        mission.setDateDepart(departDate);
        mission.setDateRetour(retourDate);
        mission.setMoyenTransport(moyenTransport);
        mission.setCombine(combinee);
        // Only attach a wished vehicle when using a service vehicle;
        // personal transport means the employee brings their own.
        if (vehicleId != null && "vehicule_de_service".equals(moyenTransport)) {
            mission.setVehicle(vehicleService.findById(vehicleId));
        }

        List<UUID> participantIds;
        if (participants != null) {
            participantIds = new java.util.ArrayList<>();
            for (String p : participants) {
                try {
                    participantIds.add(UUID.fromString(p));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed participant ids (defensive against tampered input)
                }
            }
        } else {
            participantIds = List.of();
        }

        try {
            mission = missionService.create(mission, user.getId(), participantIds);
            loginAttemptService.recordMissionCreated(user.getId());
        } catch (MissionService.ApprovalRoutingException ex) {
            errors.put("routing", ex.getMessage());
            model.addAttribute("errors", errors);
            model.addAttribute("user", user);
            model.addAttribute("vehicles", vehicleService.findAll());
            model.addAttribute("employees", employeeService.findAll());
            model.addAttribute("submittedObjet", objet);
            model.addAttribute("submittedDestination", destination);
            model.addAttribute("submittedDateDepart", dateDepart);
            model.addAttribute("submittedDateRetour", dateRetour);
            model.addAttribute("submittedMoyenTransport", moyenTransport);
            model.addAttribute("submittedVehicleId", vehicleId);
            model.addAttribute("combinee", combinee);
            return "employee/mission-form";
        }
        return "redirect:/employee/mission/" + mission.getId();
    }

    @GetMapping("/mission/{id}")
    public String missionDetail(@PathVariable UUID id, HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("user");
        Mission mission = missionService.requireOwnedBy(id, user.getId());
        if (mission == null) return "redirect:/employee/dashboard";
        model.addAttribute("user", user);
        model.addAttribute("mission", mission);
        return "employee/mission-detail";
    }

    @GetMapping("/mission/{id}/print")
    public String printMission(@PathVariable UUID id, HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("user");
        Mission mission = missionService.requireOwnedBy(id, user.getId());
        if (mission == null) return "redirect:/employee/dashboard";
        if (!"approuvee".equals(mission.getStatut())) return "redirect:/employee/mission/" + id;
        model.addAttribute("user", user);
        model.addAttribute("mission", mission);
        return "employee/mission-print";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("user");
        model.addAttribute("user", user);
        return "employee/profile";
    }
}
