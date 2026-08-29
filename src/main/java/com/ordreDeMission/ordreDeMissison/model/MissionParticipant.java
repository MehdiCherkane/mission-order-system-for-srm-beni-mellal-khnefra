package com.ordreDeMission.ordreDeMissison.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "mission_participant")
public class MissionParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    public MissionParticipant() {}

    public MissionParticipant(Mission mission, Employee employee) {
        this.mission = mission;
        this.employee = employee;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Mission getMission() { return mission; }
    public void setMission(Mission mission) { this.mission = mission; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    // Convenience fields for templates
    public String getNom() { return employee != null ? employee.getNom() : ""; }
    public String getPrenom() { return employee != null ? employee.getPrenom() : ""; }
    public String getMatricule() { return employee != null ? employee.getMatricule() : ""; }
    public String getFonction() { return employee != null ? employee.getFonction() : ""; }
    public UUID getEmployeeId() { return employee != null ? employee.getId() : null; }
    public UUID getMissionId() { return mission != null ? mission.getId() : null; }
}