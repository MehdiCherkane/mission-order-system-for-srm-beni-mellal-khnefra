package com.ordreDeMission.ordreDeMissison.repository;

import com.ordreDeMission.ordreDeMissison.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
}