package com.ordreDeMission.ordreDeMissison.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String matricule;

    @Column(length = 100)
    private String modele;

    @Column(name = "service_rattache", length = 100)
    private String serviceRattache;

    public Vehicle() {}

    public Vehicle(String matricule, String modele, String serviceRattache) {
        this.matricule = matricule;
        this.modele = modele;
        this.serviceRattache = serviceRattache;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    public String getModele() { return modele; }
    public void setModele(String modele) { this.modele = modele; }
    public String getServiceRattache() { return serviceRattache; }
    public void setServiceRattache(String serviceRattache) { this.serviceRattache = serviceRattache; }
}