package com.ordreDeMission.ordreDeMissison.service;

import com.ordreDeMission.ordreDeMissison.model.Employee;
import com.ordreDeMission.ordreDeMissison.repository.ApprovalStepRepository;
import com.ordreDeMission.ordreDeMissison.repository.EmployeeRepository;
import com.ordreDeMission.ordreDeMissison.repository.MissionParticipantRepository;
import com.ordreDeMission.ordreDeMissison.repository.MissionRepository;
import com.ordreDeMission.ordreDeMissison.repository.ServiceApproverRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EmployeeAdminService {

    public enum DeleteResult { OK, NOT_FOUND, HAS_HISTORY, ASSIGNED_TO_SERVICE }

    private final EmployeeRepository employeeRepo;
    private final MissionRepository missionRepo;
    private final ApprovalStepRepository approvalStepRepo;
    private final MissionParticipantRepository participantRepo;
    private final ServiceApproverRepository serviceApproverRepo;
    private final PasswordEncoder passwordEncoder;

    public EmployeeAdminService(EmployeeRepository employeeRepo, MissionRepository missionRepo,
                                ApprovalStepRepository approvalStepRepo,
                                MissionParticipantRepository participantRepo,
                                ServiceApproverRepository serviceApproverRepo,
                                PasswordEncoder passwordEncoder) {
        this.employeeRepo = employeeRepo;
        this.missionRepo = missionRepo;
        this.approvalStepRepo = approvalStepRepo;
        this.participantRepo = participantRepo;
        this.serviceApproverRepo = serviceApproverRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean matriculeExists(String matricule) {
        return employeeRepo.findByMatricule(matricule).isPresent();
    }

    public boolean matriculeExistsExcept(String matricule, UUID exceptId) {
        return employeeRepo.findByMatricule(matricule)
                .filter(e -> !e.getId().equals(exceptId))
                .isPresent();
    }

    public Employee create(Employee e, String rawPassword) {
        e.setMotDePasseHash(passwordEncoder.encode(rawPassword));
        return employeeRepo.save(e);
    }

    public Employee update(Employee e) {
        return employeeRepo.save(e);
    }

    public void resetPassword(UUID id, String rawPassword) {
        Employee e = employeeRepo.findById(id).orElseThrow();
        e.setMotDePasseHash(passwordEncoder.encode(rawPassword));
        employeeRepo.save(e);
    }

    public boolean hasMissionHistory(UUID employeeId) {
        if (!missionRepo.findByRequesterIdOrderByDateCreationDesc(employeeId).isEmpty()) return true;
        if (!approvalStepRepo.findAll().isEmpty()) {
            boolean inAnyStep = approvalStepRepo.findAll().stream()
                    .anyMatch(s -> s.getApprover() != null && s.getApprover().getId().equals(employeeId));
            if (inAnyStep) return true;
        }
        if (!participantRepo.findAll().isEmpty()) {
            boolean inAnyPart = participantRepo.findAll().stream()
                    .anyMatch(p -> p.getEmployee() != null && p.getEmployee().getId().equals(employeeId));
            if (inAnyPart) return true;
        }
        return false;
    }

    public DeleteResult delete(UUID id) {
        Employee e = employeeRepo.findById(id).orElse(null);
        if (e == null) return DeleteResult.NOT_FOUND;
        if (hasMissionHistory(id)) return DeleteResult.HAS_HISTORY;
        if (serviceApproverRepo.existsByChefId(id)) return DeleteResult.ASSIGNED_TO_SERVICE;
        employeeRepo.delete(e);
        return DeleteResult.OK;
    }
}
