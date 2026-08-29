package com.ordreDeMission.ordreDeMissison.repository;

import com.ordreDeMission.ordreDeMissison.model.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MissionRepository extends JpaRepository<Mission, UUID> {
    List<Mission> findByRequesterIdOrderByDateCreationDesc(UUID requesterId);
    List<Mission> findByStatut(String statut);

    @Query("SELECT DISTINCT m FROM Mission m JOIN m.approvalSteps s WHERE s.approver.id = :approverId AND s.ordre = :ordre AND s.statut = 'en_attente' ORDER BY m.dateCreation DESC")
    List<Mission> findByApproverIdAndStepOrdre(UUID approverId, int ordre);

    @Query("SELECT DISTINCT m FROM Mission m JOIN m.approvalSteps s WHERE s.approver.id = :approverId AND s.ordre = :ordre AND s.statut <> 'en_attente' ORDER BY m.dateCreation DESC")
    List<Mission> findProcessedByApproverIdAndStepOrdre(UUID approverId, int ordre);

    @Query("SELECT DISTINCT m FROM Mission m JOIN m.approvalSteps s WHERE s.approver.id = :approverId AND s.ordre = :ordre ORDER BY m.dateCreation DESC")
    List<Mission> findAllByApproverIdAndStepOrdre(UUID approverId, int ordre);

    @Query("SELECT COUNT(DISTINCT m) FROM Mission m JOIN m.approvalSteps s WHERE s.approver.id = :approverId AND s.ordre = :ordre AND s.statut = 'en_attente'")
    long countPendingByApproverIdAndStepOrdre(UUID approverId, int ordre);

    @Query("SELECT COUNT(DISTINCT m) FROM Mission m JOIN m.approvalSteps s WHERE s.approver.id = :approverId AND s.ordre = :ordre AND s.statut <> 'en_attente'")
    long countProcessedByApproverIdAndStepOrdre(UUID approverId, int ordre);

    @Query("SELECT COUNT(DISTINCT m) FROM Mission m JOIN m.approvalSteps s WHERE s.approver.id = :approverId AND s.ordre = :ordre")
    long countAllByApproverIdAndStepOrdre(UUID approverId, int ordre);

    List<Mission> findByVehicleId(UUID vehicleId);
}