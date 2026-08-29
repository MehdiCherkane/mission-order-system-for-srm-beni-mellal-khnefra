package com.ordreDeMission.ordreDeMissison.service;

import com.ordreDeMission.ordreDeMissison.model.ApprovalStep;
import com.ordreDeMission.ordreDeMissison.model.Employee;
import com.ordreDeMission.ordreDeMissison.model.Mission;
import com.ordreDeMission.ordreDeMissison.repository.ApprovalStepRepository;
import com.ordreDeMission.ordreDeMissison.repository.EmployeeRepository;
import com.ordreDeMission.ordreDeMissison.repository.MissionParticipantRepository;
import com.ordreDeMission.ordreDeMissison.repository.MissionRepository;
import com.ordreDeMission.ordreDeMissison.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionServiceRoutingTests {

    @Mock private MissionRepository missionRepo;
    @Mock private ApprovalStepRepository approvalStepRepo;
    @Mock private MissionParticipantRepository participantRepo;
    @Mock private EmployeeRepository employeeRepo;
    @Mock private VehicleRepository vehicleRepo;
    @Mock private SystemConfigService systemConfigService;
    @Mock private ServiceApproverService serviceApproverService;
    @Mock private MissionNotificationService notificationService;

    private MissionService missionService;

    @BeforeEach
    void setUp() {
        missionService = new MissionService(missionRepo, approvalStepRepo, participantRepo, employeeRepo,
                vehicleRepo, systemConfigService, serviceApproverService, notificationService);
        when(missionRepo.save(any(Mission.class))).thenAnswer(invocation -> {
            Mission mission = invocation.getArgument(0);
            if (mission.getId() == null) mission.setId(UUID.randomUUID());
            return mission;
        });
        when(approvalStepRepo.save(any(ApprovalStep.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createUsesTheChefMappedToRequestersCurrentService() {
        Employee requester = employee("EMP001", "Service Eau", "EMPLOYEE");
        Employee waterChef = employee("CHEF-EAU", "Service Eau", "CHEF_HIERARCHIQUE");
        Employee director = employee("DIR001", "Direction Provinciale", "DIRECTEUR");
        Mission mission = mission();

        when(employeeRepo.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(serviceApproverService.findChefForService("Service Eau")).thenReturn(Optional.of(waterChef));
        when(systemConfigService.get(SystemConfigService.KEY_DEFAULT_DIRECTEUR)).thenReturn("DIR001");
        when(employeeRepo.findByMatricule("DIR001")).thenReturn(Optional.of(director));

        Mission created = missionService.create(mission, requester.getId(), List.of());

        assertEquals(waterChef.getId(), approverFor(created, 1).getId());
        assertEquals(director.getId(), approverFor(created, 2).getId());
        verify(notificationService).missionSubmitted(created, waterChef);
    }

    @Test
    void createFallsBackToTheGlobalChefWhenServiceHasNoMapping() {
        Employee requester = employee("EMP002", "Service Technique", "EMPLOYEE");
        Employee fallbackChef = employee("CHEF001", "Direction Provinciale", "CHEF_HIERARCHIQUE");
        Employee director = employee("DIR001", "Direction Provinciale", "DIRECTEUR");
        Mission mission = mission();

        when(employeeRepo.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(serviceApproverService.findChefForService("Service Technique")).thenReturn(Optional.empty());
        when(systemConfigService.get(SystemConfigService.KEY_DEFAULT_CHEF)).thenReturn("CHEF001");
        when(systemConfigService.get(SystemConfigService.KEY_DEFAULT_DIRECTEUR)).thenReturn("DIR001");
        when(employeeRepo.findByMatricule("CHEF001")).thenReturn(Optional.of(fallbackChef));
        when(employeeRepo.findByMatricule("DIR001")).thenReturn(Optional.of(director));

        Mission created = missionService.create(mission, requester.getId(), List.of());

        assertEquals(fallbackChef.getId(), approverFor(created, 1).getId());
        verify(serviceApproverService).findChefForService("Service Technique");
        verify(notificationService).missionSubmitted(eq(created), eq(fallbackChef));
    }

    @Test
    void chefApprovalNotifiesRequesterAndAssignedDirector() {
        Employee requester = employee("EMP001", "Service Eau", "EMPLOYEE");
        Employee chef = employee("CHEF001", "Service Eau", "CHEF_HIERARCHIQUE");
        Employee director = employee("DIR001", "Direction Provinciale", "DIRECTEUR");
        Mission mission = stagedMission(requester, chef, "en_attente", director, "en_attente");

        when(missionRepo.findById(mission.getId())).thenReturn(Optional.of(mission));

        missionService.approveByChef(mission.getId(), null, "approuve", null);

        assertEquals("en_attente_directeur", mission.getStatut());
        verify(notificationService).chefApproved(mission, director);
    }

    @Test
    void chefRejectionNotifiesRequester() {
        Employee requester = employee("EMP001", "Service Eau", "EMPLOYEE");
        Employee chef = employee("CHEF001", "Service Eau", "CHEF_HIERARCHIQUE");
        Employee director = employee("DIR001", "Direction Provinciale", "DIRECTEUR");
        Mission mission = stagedMission(requester, chef, "en_attente", director, "en_attente");

        when(missionRepo.findById(mission.getId())).thenReturn(Optional.of(mission));

        missionService.approveByChef(mission.getId(), null, "rejete", "Informations manquantes");

        assertEquals("rejetee", mission.getStatut());
        assertEquals("annulee", mission.getStepStatut(2));
        verify(notificationService).chefRejected(mission, "Informations manquantes");
    }

    @Test
    void directorApprovalNotifiesRequester() {
        Employee requester = employee("EMP001", "Service Eau", "EMPLOYEE");
        Employee chef = employee("CHEF001", "Service Eau", "CHEF_HIERARCHIQUE");
        Employee director = employee("DIR001", "Direction Provinciale", "DIRECTEUR");
        Mission mission = stagedMission(requester, chef, "approuve", director, "en_attente");

        when(missionRepo.findById(mission.getId())).thenReturn(Optional.of(mission));

        missionService.approveByDirecteur(mission.getId(), "approuve", null);

        assertEquals("approuvee", mission.getStatut());
        verify(notificationService).directeurApproved(mission);
    }

    @Test
    void directorRejectionNotifiesRequester() {
        Employee requester = employee("EMP001", "Service Eau", "EMPLOYEE");
        Employee chef = employee("CHEF001", "Service Eau", "CHEF_HIERARCHIQUE");
        Employee director = employee("DIR001", "Direction Provinciale", "DIRECTEUR");
        Mission mission = stagedMission(requester, chef, "approuve", director, "en_attente");

        when(missionRepo.findById(mission.getId())).thenReturn(Optional.of(mission));

        missionService.approveByDirecteur(mission.getId(), "rejete", "Dates incompatibles");

        assertEquals("rejetee", mission.getStatut());
        verify(notificationService).directeurRejected(mission, "Dates incompatibles");
    }

    private static Employee employee(String matricule, String service, String role) {
        Employee employee = new Employee(matricule, "Nom", "Prénom", "Fonction", service,
                "Direction", role, matricule.toLowerCase() + "@srm.ma", "hash");
        employee.setId(UUID.randomUUID());
        return employee;
    }

    private static Mission mission() {
        Mission mission = new Mission();
        mission.setObjet("Inspection des installations de réseau");
        mission.setDestination("Beni Mellal");
        mission.setDateDepart(LocalDateTime.of(2026, 9, 10, 8, 0));
        mission.setDateRetour(LocalDateTime.of(2026, 9, 10, 17, 0));
        mission.setMoyenTransport("vehicule_de_service");
        return mission;
    }

    private static Mission stagedMission(Employee requester, Employee chef, String chefStatus,
                                          Employee director, String directorStatus) {
        Mission mission = mission();
        mission.setId(UUID.randomUUID());
        mission.setRequester(requester);
        ApprovalStep chefStep = new ApprovalStep(mission, chef, 1);
        chefStep.setStatut(chefStatus);
        ApprovalStep directorStep = new ApprovalStep(mission, director, 2);
        directorStep.setStatut(directorStatus);
        mission.getApprovalSteps().add(chefStep);
        mission.getApprovalSteps().add(directorStep);
        return mission;
    }

    private static Employee approverFor(Mission mission, int order) {
        return mission.getApprovalSteps().stream()
                .filter(step -> step.getOrdre() == order)
                .findFirst()
                .orElseThrow()
                .getApprover();
    }
}
