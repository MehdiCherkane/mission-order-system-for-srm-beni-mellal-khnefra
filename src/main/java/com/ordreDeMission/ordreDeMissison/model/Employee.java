package com.ordreDeMission.ordreDeMissison.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String matricule;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(length = 100)
    private String fonction;

    @Column(length = 100)
    private String service;

    @Column(length = 100)
    private String direction;

    @Column(nullable = false, length = 30)
    private String role;

    @Column(length = 150)
    private String email;

    @Column(name = "mot_de_passe_hash", nullable = false, length = 255)
    private String motDePasseHash;

    public Employee() {}

    public Employee(String matricule, String nom, String prenom, String fonction, String service, String direction, String role, String email, String motDePasseHash) {
        this.matricule = matricule;
        this.nom = nom;
        this.prenom = prenom;
        this.fonction = fonction;
        this.service = service;
        this.direction = direction;
        this.role = role;
        this.email = email;
        this.motDePasseHash = motDePasseHash;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getFonction() { return fonction; }
    public void setFonction(String fonction) { this.fonction = fonction; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMotDePasseHash() { return motDePasseHash; }
    public void setMotDePasseHash(String motDePasseHash) { this.motDePasseHash = motDePasseHash; }
}