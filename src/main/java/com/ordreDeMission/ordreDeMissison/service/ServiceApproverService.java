package com.ordreDeMission.ordreDeMissison.service;

import com.ordreDeMission.ordreDeMissison.model.Employee;
import com.ordreDeMission.ordreDeMissison.model.ServiceApprover;
import com.ordreDeMission.ordreDeMissison.repository.EmployeeRepository;
import com.ordreDeMission.ordreDeMissison.repository.ServiceApproverRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Transactional
public class ServiceApproverService {

    public record MappingInput(String serviceName, String chefMatricule) {}

    private final ServiceApproverRepository mappingRepo;
    private final EmployeeRepository employeeRepo;

    public ServiceApproverService(ServiceApproverRepository mappingRepo, EmployeeRepository employeeRepo) {
        this.mappingRepo = mappingRepo;
        this.employeeRepo = employeeRepo;
    }

    public List<ServiceApprover> findAll() {
        List<ServiceApprover> mappings = mappingRepo.findAll();
        mappings.forEach(mapping -> {
            if (mapping.getChef() != null) mapping.getChef().getMatricule();
        });
        return mappings;
    }

    public List<String> findAllServiceNames() {
        Map<String, String> services = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String service : employeeRepo.findDistinctNonBlankServices()) {
            String name = normalizeName(service);
            if (name != null) services.putIfAbsent(serviceKey(name), name);
        }
        for (ServiceApprover mapping : findAll()) {
            services.putIfAbsent(mapping.getServiceKey(), mapping.getServiceName());
        }
        return new ArrayList<>(services.values());
    }

    /** Returns a usable chef only; stale mappings fall back to the global setting. */
    public Optional<Employee> findChefForService(String serviceName) {
        String key = serviceKey(serviceName);
        if (key == null) return Optional.empty();
        return mappingRepo.findByServiceKey(key)
                .map(ServiceApprover::getChef)
                .filter(Objects::nonNull)
                .filter(chef -> "CHEF_HIERARCHIQUE".equals(chef.getRole()));
    }

    public Map<String, ServiceApprover> mappingsByServiceKey() {
        Map<String, ServiceApprover> mappings = new HashMap<>();
        for (ServiceApprover mapping : findAll()) {
            mappings.put(mapping.getServiceKey(), mapping);
        }
        return mappings;
    }

    public void saveMappings(List<MappingInput> inputs) {
        if (inputs == null) return;
        for (MappingInput input : inputs) {
            String serviceName = normalizeName(input.serviceName());
            if (serviceName == null) continue;
            String key = serviceKey(serviceName);
            ServiceApprover mapping = mappingRepo.findByServiceKey(key)
                    .orElseGet(() -> new ServiceApprover(serviceName, key, null));
            mapping.setServiceName(serviceName);
            mapping.setServiceKey(key);

            String chefMatricule = normalizeName(input.chefMatricule());
            if (chefMatricule == null) {
                mapping.setChef(null);
            } else {
                Employee chef = employeeRepo.findByMatricule(chefMatricule).orElse(null);
                if (chef == null || !"CHEF_HIERARCHIQUE".equals(chef.getRole())) {
                    throw new IllegalArgumentException("Le chef sélectionné est invalide pour le service « " + serviceName + " ».");
                }
                mapping.setChef(chef);
            }
            mappingRepo.save(mapping);
        }
    }

    public boolean isChefAssignedToAService(UUID employeeId) {
        return employeeId != null && mappingRepo.existsByChefId(employeeId);
    }

    public static String serviceKey(String serviceName) {
        String name = normalizeName(serviceName);
        return name == null ? null : name.toLowerCase(Locale.ROOT);
    }

    private static String normalizeName(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
