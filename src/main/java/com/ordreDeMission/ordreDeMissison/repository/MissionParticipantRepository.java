package com.ordreDeMission.ordreDeMissison.repository;

import com.ordreDeMission.ordreDeMissison.model.MissionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MissionParticipantRepository extends JpaRepository<MissionParticipant, UUID> {
}