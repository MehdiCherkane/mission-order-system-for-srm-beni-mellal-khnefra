package com.ordreDeMission.ordreDeMissison.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "approval_step")
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private Employee approver;

    @Column(nullable = false)
    private int ordre;

    @Column(nullable = false, length = 30)
    private String statut;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_action")
    private LocalDateTime dateAction;

    public ApprovalStep() {
        this.statut = "en_attente";
    }

    public ApprovalStep(Mission mission, Employee approver, int ordre) {
        this.mission = mission;
        this.approver = approver;
        this.ordre = ordre;
        this.statut = "en_attente";
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Mission getMission() { return mission; }
    public void setMission(Mission mission) { this.mission = mission; }
    public Employee getApprover() { return approver; }
    public void setApprover(Employee approver) { this.approver = approver; }
    public UUID getApproverId() { return approver != null ? approver.getId() : null; }
    public String getApproverNom() { return approver != null ? approver.getPrenom() + " " + approver.getNom() : ""; }
    public String getApproverRoleLabel() {
        if (approver == null) return "";
        return switch (approver.getRole()) {
            case "CHEF_HIERARCHIQUE" -> "Chef hi\u00e9rarchique";
            case "DIRECTEUR" -> "Directeur provincial";
            default -> approver.getRole();
        };
    }
    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public LocalDateTime getDateAction() { return dateAction; }
    public void setDateAction(LocalDateTime dateAction) { this.dateAction = dateAction; }
}