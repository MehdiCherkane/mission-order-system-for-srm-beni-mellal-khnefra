package com.ordreDeMission.ordreDeMissison.repository;

import com.ordreDeMission.ordreDeMissison.model.ServiceApprover;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceApproverRepository extends JpaRepository<ServiceApprover, UUID> {
    Optional<ServiceApprover> findByServiceKey(String serviceKey);
    boolean existsByChefId(UUID chefId);
}
