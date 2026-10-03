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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionServiceAuthorizationTests {

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
    }

    @Test
    void requireOwnedByReturnsMissionForOwner() {
        Employee owner = employee("EMP001", "EMPLOYEE");
        Mission mission = ownedMission(owner);

        when(missionRepo.findById(mission.getId())).thenReturn(Optional.of(mission));

        assertSame(mission, missionService.requireOwnedBy(mission.getId(), owner.getId()));
    }

    @Test
    void requireOwnedByDeniesStranger() {
        Employee owner = employee("EMP001", "EMPLOYEE");
        Employee stranger = employee("EMP002", "EMPLOYEE");
        Mission mission = ownedMission(owner);

        when(missionRepo.findById(mission.getId())).thenReturn(Optional.of(mission));

        assertThrows(AccessDeniedException.class,
                () -> missionService.requireOwnedBy(mission.getId(), stranger.getId()));
    }

    @Test
    void requireOwnedByReturnsNullForUnknownId() {
        UUID unknown = UUID.randomUUID();
        when(missionRepo.findById(unknown)).thenReturn(Optional.empty());

        assertNull(missionService.requireOwnedBy(unknown, UUID.randomUUID()));
    }

    @Test
    void requireAssignedApproverReturnsMissionForAssignedChef() {
        Employee requester = employee("EMP001", "EMPLOYEE");
        Employee chef = employee("CHEF001", "CHEF_HIERARCHIQUE");
        Employee directeur = employee("DIR001", "DIRECTEUR");
        Mission mission = stagedMission(requester, chef, directeur);

        when(missionRepo.findById(mission.getId())).thenReturn(Optional.of(mission));

        assertSame(mission, missionService.requireAssignedApprover(mission.getId(), 1, chef.getId()));
    }

    @Test
    void requireAssignedApproverDeniesWrongEmployee() {
        Employee requester = employee("EMP001", "EMPLOYEE");
        Employee chef = employee("CHEF001", "CHEF_HIERARCHIQUE");
        Employee otherChef = employee("CHEF002", "CHEF_HIERARCHIQUE");
        Employee directeur = employee("DIR001", "DIRECTEUR");
        Mission mission = stagedMission(requester, chef, directeur);

        when(missionRepo.findById(mission.getId())).thenReturn(Optional.of(mission));

        assertThrows(AccessDeniedException.class,
                () -> missionService.requireAssignedApprover(mission.getId(), 1, otherChef.getId()));
    }

    @Test
    void requireAssignedApproverDeniesChefOnDirecteurStep() {
        Employee requester = employee("EMP001", "EMPLOYEE");
        Employee chef = employee("CHEF001", "CHEF_HIERARCHIQUE");
        Employee directeur = employee("DIR001", "DIRECTEUR");
        Mission mission = stagedMission(requester, chef, directeur);

        when(missionRepo.findById(mission.getId())).thenReturn(Optional.of(mission));

        assertThrows(AccessDeniedException.class,
                () -> missionService.requireAssignedApprover(mission.getId(), 2, chef.getId()));
    }

    @Test
    void requireAssignedApproverReturnsNullForUnknownId() {
        UUID unknown = UUID.randomUUID();
        when(missionRepo.findById(unknown)).thenReturn(Optional.empty());

        assertNull(missionService.requireAssignedApprover(unknown, 1, UUID.randomUUID()));
    }

    private static Employee employee(String matricule, String role) {
        Employee employee = new Employee(matricule, "Nom", "Prénom", "Fonction", "Service",
                "Direction", role, matricule.toLowerCase() + "@srm.ma", "hash");
        employee.setId(UUID.randomUUID());
        return employee;
    }

    private static Mission ownedMission(Employee owner) {
        Mission mission = new Mission();
        mission.setId(UUID.randomUUID());
        mission.setRequester(owner);
        mission.setObjet("Inspection des installations de réseau");
        mission.setDestination("Beni Mellal");
        mission.setDateDepart(LocalDateTime.of(2026, 9, 10, 8, 0));
        mission.setDateRetour(LocalDateTime.of(2026, 9, 10, 17, 0));
        mission.setMoyenTransport("vehicule_de_service");
        return mission;
    }

    private static Mission stagedMission(Employee requester, Employee chef, Employee directeur) {
        Mission mission = ownedMission(requester);
        mission.getApprovalSteps().add(new ApprovalStep(mission, chef, 1));
        mission.getApprovalSteps().add(new ApprovalStep(mission, directeur, 2));
        return mission;
    }
}
