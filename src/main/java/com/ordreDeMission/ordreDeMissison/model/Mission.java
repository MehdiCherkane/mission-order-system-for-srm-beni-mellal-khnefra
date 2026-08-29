package com.ordreDeMission.ordreDeMissison.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mission")
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private Employee requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String objet;

    @Column(nullable = false, length = 255)
    private String destination;

    @Column(name = "date_depart", nullable = false)
    private LocalDateTime dateDepart;

    @Column(name = "date_retour", nullable = false)
    private LocalDateTime dateRetour;

    @Column(name = "moyen_transport", length = 50)
    private String moyenTransport;

    @Column(nullable = false, length = 30)
    private String statut;

    @Column(nullable = false)
    private boolean combinee;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @OneToMany(mappedBy = "mission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApprovalStep> approvalSteps = new ArrayList<>();

    @OneToMany(mappedBy = "mission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MissionParticipant> participants = new ArrayList<>();

    public Mission() {
        this.statut = "soumise";
        this.dateCreation = LocalDateTime.now();
    }

    // Helper fields (not persisted — derived from relationships for frontend)
    public String getRequesterNom() { return requester != null ? requester.getNom() : ""; }
    public String getRequesterPrenom() { return requester != null ? requester.getPrenom() : ""; }
    public String getRequesterMatricule() { return requester != null ? requester.getMatricule() : ""; }
    public String getRequesterFonction() { return requester != null ? requester.getFonction() : ""; }
    public String getRequesterService() { return requester != null ? requester.getService() : ""; }
    public String getRequesterDirection() { return requester != null ? requester.getDirection() : ""; }
    public String getVehicleInfo() {
        return vehicle != null ? vehicle.getMatricule() + " - " + vehicle.getModele() : null;
    }

    public String getMissionNumber() {
        if (id == null) return "";
        return id.toString().substring(0, 6).toUpperCase();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Employee getRequester() { return requester; }
    public void setRequester(Employee requester) { this.requester = requester; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    public UUID getVehicleId() { return vehicle != null ? vehicle.getId() : null; }
    public void setVehicleId(UUID vehicleId) { /* set via vehicle field; kept for API symmetry */ }
    public String getObjet() { return objet; }
    public void setObjet(String objet) { this.objet = objet; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalDateTime getDateDepart() { return dateDepart; }
    public void setDateDepart(LocalDateTime dateDepart) { this.dateDepart = dateDepart; }
    public LocalDateTime getDateRetour() { return dateRetour; }
    public void setDateRetour(LocalDateTime dateRetour) { this.dateRetour = dateRetour; }
    public String getMoyenTransport() { return moyenTransport; }
    public void setMoyenTransport(String moyenTransport) { this.moyenTransport = moyenTransport; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public boolean isCombine() { return combinee; }
    public void setCombine(boolean combinee) { this.combinee = combinee; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public List<ApprovalStep> getApprovalSteps() { return approvalSteps; }
    public void setApprovalSteps(List<ApprovalStep> approvalSteps) { this.approvalSteps = approvalSteps; }
    public List<MissionParticipant> getParticipants() { return participants; }
    public void setParticipants(List<MissionParticipant> participants) { this.participants = participants; }

    public String getStatutLabel() {
        return switch (statut) {
            case "soumise" -> "Soumise";
            case "en_attente_chef" -> "En attente";
            case "en_attente_directeur" -> "En attente";
            case "approuvee" -> "Approuv\u00e9e";
            case "rejetee" -> "Rejet\u00e9e";
            default -> statut;
        };
    }

    public String getStatutCss() {
        return switch (statut) {
            case "soumise" -> "submitted";
            case "en_attente_chef", "en_attente_directeur" -> "under-review";
            case "approuvee" -> "accepted";
            case "rejetee" -> "rejected";
            default -> "draft";
        };
    }

    public String getCurrentApproverName() {
        if (approvalSteps == null) return "";
        // Don't show a "current approver" for terminal mission states
        if ("approuvee".equals(statut) || "rejetee".equals(statut)) return "";
        return approvalSteps.stream()
                .filter(s -> "en_attente".equals(s.getStatut()))
                .findFirst()
                .map(s -> s.getApproverRoleLabel())
                .orElse("");
    }

    public String getStepStatut(int ordre) {
        if (approvalSteps == null) return "";
        return approvalSteps.stream()
                .filter(s -> s.getOrdre() == ordre)
                .findFirst()
                .map(ApprovalStep::getStatut)
                .orElse("");
    }

    public String getStepStatutLabel(int ordre) {
        return switch (getStepStatut(ordre)) {
            case "en_attente" -> "En attente";
            case "approuve" -> "Approuv\u00e9e";
            case "rejete" -> "Rejet\u00e9e";
            case "annulee" -> "Annul\u00e9e";
            default -> "";
        };
    }

    public String getStepStatutCss(int ordre) {
        return switch (getStepStatut(ordre)) {
            case "en_attente" -> "under-review";
            case "approuve" -> "accepted";
            case "rejete" -> "rejected";
            case "annulee" -> "cancelled";
            default -> "draft";
        };
    }
}