package com.ordreDeMission.ordreDeMissison.service;

import com.ordreDeMission.ordreDeMissison.model.Vehicle;
import com.ordreDeMission.ordreDeMissison.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class VehicleService {

    private final VehicleRepository repo;

    public VehicleService(VehicleRepository repo) { this.repo = repo; }

    public List<Vehicle> findAll() { return repo.findAll(); }

    public Vehicle findById(UUID id) { return repo.findById(id).orElse(null); }

    public Vehicle save(Vehicle v) { return repo.save(v); }

    public void delete(UUID id) { repo.deleteById(id); }
}