package com.ordreDeMission.ordreDeMissison.service;

import com.ordreDeMission.ordreDeMissison.model.Employee;
import com.ordreDeMission.ordreDeMissison.repository.EmployeeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeRepository repo;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    public Employee authenticate(String matricule, String rawPassword) {
        return repo.findByMatricule(matricule)
                .filter(e -> passwordEncoder.matches(rawPassword, e.getMotDePasseHash()))
                .orElse(null);
    }

    public Employee findByMatricule(String matricule) {
        return repo.findByMatricule(matricule).orElse(null);
    }

    public Employee findById(UUID id) {
        return repo.findById(id).orElse(null);
    }

    public List<Employee> findAll() {
        return repo.findAll();
    }

    public List<Employee> search(String query) {
        if (query == null || query.isBlank()) return List.of();
        String q = query.toLowerCase();
        return repo.findAll().stream()
                .filter(e -> e.getNom().toLowerCase().contains(q)
                        || e.getPrenom().toLowerCase().contains(q)
                        || e.getMatricule().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }
}