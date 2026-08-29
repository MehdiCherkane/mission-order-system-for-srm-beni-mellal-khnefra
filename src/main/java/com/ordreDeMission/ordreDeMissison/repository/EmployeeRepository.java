package com.ordreDeMission.ordreDeMissison.repository;

import com.ordreDeMission.ordreDeMissison.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByMatricule(String matricule);

    @Query("select distinct e.service from Employee e where e.service is not null and trim(e.service) <> ''")
    List<String> findDistinctNonBlankServices();
}
