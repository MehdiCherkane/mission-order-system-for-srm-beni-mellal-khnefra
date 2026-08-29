package com.ordreDeMission.ordreDeMissison.repository;

import com.ordreDeMission.ordreDeMissison.model.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, UUID> {
    List<ApprovalStep> findByMissionIdOrderByOrdre(UUID missionId);
}