package com.evlaleyla.gotournamentmanager.backend.participant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByEmailIgnoreCase(String email);
}