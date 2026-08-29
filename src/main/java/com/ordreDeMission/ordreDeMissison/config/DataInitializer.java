package com.ordreDeMission.ordreDeMissison.config;

import com.ordreDeMission.ordreDeMissison.model.*;
import com.ordreDeMission.ordreDeMissison.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepo;
    private final VehicleRepository vehicleRepo;
    private final MissionRepository missionRepo;
    private final ApprovalStepRepository approvalStepRepo;
    private final MissionParticipantRepository participantRepo;
    private final SystemConfigRepository systemConfigRepo;
    private final ServiceApproverRepository serviceApproverRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(EmployeeRepository employeeRepo, VehicleRepository vehicleRepo,
                           MissionRepository missionRepo, ApprovalStepRepository approvalStepRepo,
                           MissionParticipantRepository participantRepo,
                           SystemConfigRepository systemConfigRepo, ServiceApproverRepository serviceApproverRepo,
                           PasswordEncoder passwordEncoder) {
        this.employeeRepo = employeeRepo;
        this.vehicleRepo = vehicleRepo;
        this.missionRepo = missionRepo;
        this.approvalStepRepo = approvalStepRepo;
        this.participantRepo = participantRepo;
        this.systemConfigRepo = systemConfigRepo;
        this.serviceApproverRepo = serviceApproverRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (employeeRepo.count() == 0) {
            seedData();
        }
        seedSystemConfig();
        seedServiceApprovers();
    }

    private void seedSystemConfig() {
        if (systemConfigRepo.findById("default_chef_matricule").isEmpty()) {
            systemConfigRepo.save(new com.ordreDeMission.ordreDeMissison.model.SystemConfig(
                    "default_chef_matricule", "CHEF001", "Matricule du chef hi\u00e9rarchique par d\u00e9faut"));
        }
        if (systemConfigRepo.findById("default_directeur_matricule").isEmpty()) {
            systemConfigRepo.save(new com.ordreDeMission.ordreDeMissison.model.SystemConfig(
                    "default_directeur_matricule", "DIR001", "Matricule du directeur provincial par d\u00e9faut"));
        }
    }

    private void seedServiceApprovers() {
        if (serviceApproverRepo.count() > 0) return;
        employeeRepo.findByMatricule("CHEF001").ifPresent(chef ->
                serviceApproverRepo.save(new ServiceApprover("Service Eau", "service eau", chef)));
        employeeRepo.findByMatricule("CHEF002").ifPresent(chef ->
                serviceApproverRepo.save(new ServiceApprover("Service Électricité", "service électricité", chef)));
    }

    private void seedData() {

        String pwd = passwordEncoder.encode("password");

        Employee emp1 = employeeRepo.save(new Employee("EMP001", "Benali", "Ahmed", "Technicien", "Service Eau", "Direction Technique", "EMPLOYEE", "ahmed.benali@srm.ma", pwd));
        Employee emp2 = employeeRepo.save(new Employee("EMP002", "Zahra", "Fatima", "Ingénieur", "Service Électricité", "Direction Technique", "EMPLOYEE", "fatima.zahra@srm.ma", pwd));
        Employee emp3 = employeeRepo.save(new Employee("EMP003", "Amrani", "Youssef", "Technicien", "Service Eau", "Direction Technique", "EMPLOYEE", "youssef.amrani@srm.ma", pwd));
        Employee chef = employeeRepo.save(new Employee("CHEF001", "Idrissi", "Karim", "Chef de Service", "Service Technique", "Direction Provinciale", "CHEF_HIERARCHIQUE", "karim.idrissi@srm.ma", pwd));
        Employee chef2 = employeeRepo.save(new Employee("CHEF002", "Alaoui", "Sara", "Chef de Service", "Service Électricité", "Direction Technique", "CHEF_HIERARCHIQUE", "sara.alaoui@srm.ma", pwd));
        Employee directeur = employeeRepo.save(new Employee("DIR001", "El Amrani", "Nadia", "Directeur Provincial", "Direction Provinciale", "Direction Provinciale", "DIRECTEUR", "nadia.elamrani@srm.ma", pwd));
        Employee admin = employeeRepo.save(new Employee("ADMIN001", "Admin", "System", "Administrateur", "Service Informatique", "Direction Générale", "ADMIN", "admin@srm.ma", pwd));

        Vehicle v1 = vehicleRepo.save(new Vehicle("1234-A-5678", "Dacia Logan", "Service Technique"));
        Vehicle v2 = vehicleRepo.save(new Vehicle("5678-B-1234", "Renault Kangoo", "Service Eau"));
        Vehicle v3 = vehicleRepo.save(new Vehicle("9012-C-3456", "Toyota Hilux", "Service Électricité"));
        Vehicle v4 = vehicleRepo.save(new Vehicle("3456-D-7890", "Peugeot Partner", "Service Technique"));
        Vehicle v5 = vehicleRepo.save(new Vehicle("7890-E-1234", "Ford Transit", "Direction Provinciale"));

        Mission m1 = createMission(emp1, "Réparation de fuite sur le réseau d'eau potable", "Beni Mellal",
                LocalDateTime.of(2026, 7, 30, 8, 0), LocalDateTime.of(2026, 7, 30, 17, 0),
                "vehicule_de_service", v1, false);
        addStep(m1, chef, 1, "approuve", null, LocalDateTime.of(2026, 7, 28, 10, 0));
        m1.setStatut("en_attente_directeur");
        missionRepo.save(m1);

        Mission m2 = createMission(emp1, "Collecte d'échantillons d'eau", "Khenifra",
                LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 15, 0),
                "vehicule_de_service", v2, false);
        addStep(m2, chef, 1, "approuve", null, LocalDateTime.of(2026, 7, 28, 11, 30));
        addStep(m2, directeur, 2, "approuve", null, LocalDateTime.of(2026, 7, 29, 9, 0));
        m2.setStatut("approuvee");
        missionRepo.save(m2);

        Mission m3 = createMission(emp1, "Maintenance des équipements électriques", "Fkih Ben Salah",
                LocalDateTime.of(2026, 7, 25, 7, 30), LocalDateTime.of(2026, 7, 25, 18, 0),
                "vehicule_personnel", null, false);
        addStep(m3, chef, 1, "rejete", "Véhicule personnel non autorisé pour cette distance.", LocalDateTime.of(2026, 7, 24, 14, 0));
        m3.setStatut("rejetee");
        missionRepo.save(m3);

        Mission m4 = createMission(emp1, "Inspection des installations", "Azilal",
                LocalDateTime.of(2026, 8, 5, 8, 0), LocalDateTime.of(2026, 8, 5, 16, 0),
                "vehicule_de_service", v3, true);
        addStep(m4, chef, 1, "en_attente", null, null);
        addStep(m4, directeur, 2, "en_attente", null, null);
        m4.setStatut("en_attente_chef");
        missionRepo.save(m4);
        addParticipant(m4, emp3);

        Mission m5 = createMission(emp2, "Réunion technique régionale", "Beni Mellal",
                LocalDateTime.of(2026, 8, 10, 10, 0), LocalDateTime.of(2026, 8, 10, 16, 0),
                "vehicule_de_service", null, false);
        addStep(m5, chef, 1, "en_attente", null, null);
        addStep(m5, directeur, 2, "en_attente", null, null);
        m5.setStatut("en_attente_chef");
        missionRepo.save(m5);

        Mission m6 = createMission(emp2, "Formation sur les nouveaux compteurs", "Casablanca",
                LocalDateTime.of(2026, 8, 15, 9, 0), LocalDateTime.of(2026, 8, 16, 17, 0),
                "vehicule_de_service", v1, false);
        addStep(m6, chef, 1, "approuve", null, LocalDateTime.of(2026, 7, 28, 15, 0));
        addStep(m6, directeur, 2, "approuve", null, LocalDateTime.of(2026, 7, 29, 11, 0));
        m6.setStatut("approuvee");
        missionRepo.save(m6);
    }

    private Mission createMission(Employee requester, String objet, String destination,
                                   LocalDateTime depart, LocalDateTime retour,
                                   String moyenTransport, Vehicle vehicle, boolean combinee) {
        Mission m = new Mission();
        m.setRequester(requester);
        m.setObjet(objet);
        m.setDestination(destination);
        m.setDateDepart(depart);
        m.setDateRetour(retour);
        m.setMoyenTransport(moyenTransport);
        m.setVehicle(vehicle);
        m.setCombine(combinee);
        m.setStatut("soumise");
        m.setDateCreation(LocalDateTime.now());
        return missionRepo.save(m);
    }

    private void addStep(Mission mission, Employee approver, int ordre, String statut, String commentaire, LocalDateTime date) {
        ApprovalStep step = new ApprovalStep(mission, approver, ordre);
        step.setStatut(statut);
        step.setCommentaire(commentaire);
        step.setDateAction(date);
        approvalStepRepo.save(step);
    }

    private void addParticipant(Mission mission, Employee employee) {
        MissionParticipant mp = new MissionParticipant(mission, employee);
        participantRepo.save(mp);
    }
}
