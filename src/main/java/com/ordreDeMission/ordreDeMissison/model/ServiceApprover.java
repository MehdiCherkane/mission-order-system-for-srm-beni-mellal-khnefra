package com.ordreDeMission.ordreDeMissison.model;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * The chef responsible for first-level approval for a service.  Employee.service
 * remains text for backwards compatibility, while serviceKey gives that text a
 * stable, case-insensitive lookup value.
 */
@Entity
@Table(name = "service_approver", uniqueConstraints = @UniqueConstraint(columnNames = "service_key"))
public class ServiceApprover {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "service_key", nullable = false, length = 120)
    private String serviceKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chef_employee_id")
    private Employee chef;

    public ServiceApprover() {
    }

    public ServiceApprover(String serviceName, String serviceKey, Employee chef) {
        this.serviceName = serviceName;
        this.serviceKey = serviceKey;
        this.chef = chef;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getServiceKey() { return serviceKey; }
    public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }
    public Employee getChef() { return chef; }
    public void setChef(Employee chef) { this.chef = chef; }
}
