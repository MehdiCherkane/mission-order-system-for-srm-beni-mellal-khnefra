package com.ordreDeMission.ordreDeMissison.service;

import com.ordreDeMission.ordreDeMissison.model.*;
import com.ordreDeMission.ordreDeMissison.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MissionService {

    private final MissionRepository missionRepo;
    private final ApprovalStepRepository approvalStepRepo;
    private final MissionParticipantRepository participantRepo;
    private final EmployeeRepository employeeRepo;
    private final VehicleRepository vehicleRepo;
    private final SystemConfigService systemConfigService;
    private final ServiceApproverService serviceApproverService;
    private final MissionNotificationService notificationService;

    public MissionService(MissionRepository missionRepo, ApprovalStepRepository approvalStepRepo,
                          MissionParticipantRepository participantRepo, EmployeeRepository employeeRepo,
                          VehicleRepository vehicleRepo, SystemConfigService systemConfigService,
                          ServiceApproverService serviceApproverService,
                          MissionNotificationService notificationService) {
        this.missionRepo = missionRepo;
        this.approvalStepRepo = approvalStepRepo;
        this.participantRepo = participantRepo;
        this.employeeRepo = employeeRepo;
        this.vehicleRepo = vehicleRepo;
        this.systemConfigService = systemConfigService;
        this.serviceApproverService = serviceApproverService;
        this.notificationService = notificationService;
    }

    public List<Mission> findByRequester(UUID requesterId) {
        return initialize(missionRepo.findByRequesterIdOrderByDateCreationDesc(requesterId));
    }

    public List<Mission> findPendingChef() {
        return initialize(missionRepo.findByStatut("en_attente_chef"));
    }

    public List<Mission> findPendingDirecteur() {
        return initialize(missionRepo.findByStatut("en_attente_directeur"));
    }

    public List<Mission> findChefMissions(UUID chefId, String status) {
        return initialize(findMissionsByStatus(chefId, 1, status));
    }

    public List<Mission> findDirecteurMissions(UUID directeurId, String status) {
        return initialize(findMissionsByStatus(directeurId, 2, status));
    }

    public record MissionCounts(long pending, long processed, long total) {}

    public static class ApprovalRoutingException extends RuntimeException {
        public ApprovalRoutingException(String message) {
            super(message);
        }
    }

    public MissionCounts getMissionCounts(UUID approverId, int ordre) {
        return new MissionCounts(
                missionRepo.countPendingByApproverIdAndStepOrdre(approverId, ordre),
                missionRepo.countProcessedByApproverIdAndStepOrdre(approverId, ordre),
                missionRepo.countAllByApproverIdAndStepOrdre(approverId, ordre));
    }

    private List<Mission> findMissionsByStatus(UUID approverId, int ordre, String status) {
        return switch (status == null ? "PENDING" : status.toUpperCase()) {
            case "PROCESSED" -> missionRepo.findProcessedByApproverIdAndStepOrdre(approverId, ordre);
            case "ALL" -> missionRepo.findAllByApproverIdAndStepOrdre(approverId, ordre);
            default -> missionRepo.findByApproverIdAndStepOrdre(approverId, ordre);
        };
    }

    public List<Mission> findAll() {
        return initialize(missionRepo.findAll());
    }

    public List<Mission> findByVehicleId(UUID vehicleId) {
        return initialize(missionRepo.findByVehicleId(vehicleId));
    }

    public Mission findById(UUID id) {
        Mission m = missionRepo.findById(id).orElse(null);
        if (m != null) initialize(List.of(m));
        return m;
    }

    public boolean isStepAlreadyProcessed(UUID missionId, int ordre) {
        Mission m = findById(missionId);
        if (m == null) return false;
        return m.getApprovalSteps().stream()
                .filter(s -> s.getOrdre() == ordre)
                .anyMatch(s -> !"en_attente".equals(s.getStatut()));
    }

    public boolean isAssignedApprover(UUID missionId, int ordre, UUID employeeId) {
        if (employeeId == null) return false;
        Mission mission = findById(missionId);
        return mission != null && mission.getApprovalSteps().stream()
                .filter(step -> step.getOrdre() == ordre)
                .map(ApprovalStep::getApprover)
                .filter(approver -> approver != null)
                .anyMatch(approver -> employeeId.equals(approver.getId()));
    }

    /**
     * Returns the mission only if it belongs to the given requester.
     * Unknown id returns null (caller redirects, no ID enumeration).
     * Known id owned by someone else throws AccessDeniedException (HTTP 403).
     */
    public Mission requireOwnedBy(UUID missionId, UUID requesterId) {
        Mission mission = findById(missionId);
        if (mission == null) return null;
        if (requesterId == null || mission.getRequester() == null
                || !requesterId.equals(mission.getRequester().getId())) {
            throw new AccessDeniedException("Accès refusé à la mission " + missionId);
        }
        return mission;
    }

    /**
     * Returns the mission only if the given employee is the assigned approver
     * for the given step. Same null/denied contract as requireOwnedBy.
     */
    public Mission requireAssignedApprover(UUID missionId, int ordre, UUID employeeId) {
        Mission mission = findById(missionId);
        if (mission == null) return null;
        if (!isAssignedApprover(missionId, ordre, employeeId)) {
            throw new AccessDeniedException("Mission " + missionId + " non affectée à cet approbateur");
        }
        return mission;
    }

    // Force eager initialization of lazy collections within the open transaction so
    // Thymeleaf can safely traverse them after the session closes.
    private List<Mission> initialize(List<Mission> missions) {
        for (Mission m : missions) {
            m.getApprovalSteps().size();
            m.getParticipants().size();
            if (m.getRequester() != null) {
                m.getRequester().getNom();
            }
            if (m.getVehicle() != null) {
                m.getVehicle().getMatricule();
            }
            for (ApprovalStep s : m.getApprovalSteps()) {
                if (s.getApprover() != null) s.getApprover().getRole();
            }
            for (MissionParticipant p : m.getParticipants()) {
                if (p.getEmployee() != null) p.getEmployee().getNom();
            }
        }
        return missions;
    }

    @Transactional
    public Mission create(Mission mission, UUID requesterId, List<UUID> participantIds) {
        Employee requester = employeeRepo.findById(requesterId).orElseThrow();
        Employee chef = resolveChef(requester);
        Employee directeur = resolveDirecteur();
        if (chef == null) {
            throw new ApprovalRoutingException("Aucun chef n'est affecté à votre service et aucun chef de secours n'est configuré.");
        }
        if (directeur == null) {
            throw new ApprovalRoutingException("Aucun directeur provincial par défaut n'est configuré.");
        }

        mission.setRequester(requester);
        mission.setStatut("en_attente_chef");
        mission.setDateCreation(LocalDateTime.now());
        // Sequential number within the creation year (restarts at 1 each year).
        // Unique constraint (annee, numero) guards against duplicates.
        int annee = mission.getDateCreation().getYear();
        Integer max = missionRepo.findMaxNumeroByAnnee(annee);
        mission.setAnnee(annee);
        mission.setNumero(max == null ? 1 : max + 1);
        mission = missionRepo.save(mission);

        ApprovalStep step1 = new ApprovalStep(mission, chef, 1);
        approvalStepRepo.save(step1);
        mission.getApprovalSteps().add(step1);
        ApprovalStep step2 = new ApprovalStep(mission, directeur, 2);
        approvalStepRepo.save(step2);
        mission.getApprovalSteps().add(step2);

        // Add participants
        if (participantIds != null) {
            for (UUID pid : participantIds) {
                Employee participant = employeeRepo.findById(pid).orElse(null);
                if (participant != null) {
                    MissionParticipant mp = new MissionParticipant(mission, participant);
                    participantRepo.save(mp);
                    mission.getParticipants().add(mp);
                }
            }
        }

        notificationService.missionSubmitted(mission, chef);
        return mission;
    }

    private Employee resolveChef(Employee requester) {
        Employee mappedChef = serviceApproverService.findChefForService(requester.getService()).orElse(null);
        if (mappedChef != null) return mappedChef;
        return findConfiguredApprover(SystemConfigService.KEY_DEFAULT_CHEF, "CHEF_HIERARCHIQUE");
    }

    private Employee resolveDirecteur() {
        return findConfiguredApprover(SystemConfigService.KEY_DEFAULT_DIRECTEUR, "DIRECTEUR");
    }

    private Employee findConfiguredApprover(String key, String role) {
        String matricule = systemConfigService.get(key);
        if (matricule == null || matricule.isBlank()) return null;
        return employeeRepo.findByMatricule(matricule.trim())
                .filter(employee -> role.equals(employee.getRole()))
                .orElse(null);
    }

    @Transactional
    public Mission approveByChef(UUID missionId, UUID vehicleId, String action, String commentaire) {
        Mission mission = missionRepo.findById(missionId).orElseThrow();
        if (!"approuve".equals(action) && !"rejete".equals(action)) return mission;
        ApprovalStep step = mission.getApprovalSteps().stream()
                .filter(s -> s.getOrdre() == 1).findFirst().orElse(null);

        if (step == null) return mission;
        if (!"en_attente".equals(step.getStatut())) return mission; // idempotency guard

        step.setStatut(action);
        step.setCommentaire(commentaire);
        step.setDateAction(LocalDateTime.now());
        approvalStepRepo.save(step);

        if (vehicleId != null) {
            Vehicle v = vehicleRepo.findById(vehicleId).orElse(null);
            if (v != null) {
                // Only assign a vehicle when the mission uses service vehicle
                if ("vehicule_de_service".equals(mission.getMoyenTransport())) {
                    mission.setVehicle(v);
                }
            }
        }

        if ("rejete".equals(action)) {
            // Cascade: cancel any later pending step(s) so the workflow stops
            for (ApprovalStep s : mission.getApprovalSteps()) {
                if (s.getOrdre() > step.getOrdre() && "en_attente".equals(s.getStatut())) {
                    s.setStatut("annulee");
                    s.setDateAction(LocalDateTime.now());
                    approvalStepRepo.save(s);
                }
            }
            mission.setStatut("rejetee");
        } else {
            mission.setStatut("en_attente_directeur");
        }
        Mission savedMission = missionRepo.save(mission);
        if ("rejete".equals(action)) {
            notificationService.chefRejected(savedMission, commentaire);
        } else {
            Employee directeur = savedMission.getApprovalSteps().stream()
                    .filter(s -> s.getOrdre() == 2)
                    .map(ApprovalStep::getApprover)
                    .findFirst()
                    .orElse(null);
            notificationService.chefApproved(savedMission, directeur);
        }
        return savedMission;
    }

    @Transactional
    public Mission approveByDirecteur(UUID missionId, String action, String commentaire) {
        Mission mission = missionRepo.findById(missionId).orElseThrow();
        if (!"approuve".equals(action) && !"rejete".equals(action)) return mission;
        ApprovalStep step = mission.getApprovalSteps().stream()
                .filter(s -> s.getOrdre() == 2).findFirst().orElse(null);

        if (step == null) return mission;

        // Refuse to act if an earlier step was rejected (workflow integrity)
        boolean earlierStepNotApproved = mission.getApprovalSteps().stream()
                .anyMatch(s -> s.getOrdre() < step.getOrdre() && !"approuve".equals(s.getStatut()));
        if (earlierStepNotApproved) return mission;
        if (!"en_attente".equals(step.getStatut())) return mission; // idempotency guard

        step.setStatut(action);
        step.setCommentaire(commentaire);
        step.setDateAction(LocalDateTime.now());
        approvalStepRepo.save(step);

        mission.setStatut("approuve".equals(action) ? "approuvee" : "rejetee");
        Mission savedMission = missionRepo.save(mission);
        if ("approuve".equals(action)) {
            notificationService.directeurApproved(savedMission);
        } else {
            notificationService.directeurRejected(savedMission, commentaire);
        }
        return savedMission;
    }
}
