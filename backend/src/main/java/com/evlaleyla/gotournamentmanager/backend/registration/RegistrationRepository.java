package com.evlaleyla.gotournamentmanager.backend.registration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    List<Registration> findAllByOrderByRegistrationDateDesc();

    List<Registration> findByTournamentIdOrderByRegistrationDateAsc(Long tournamentId);

    List<Registration> findByTournamentIdOrderByParticipantLastNameAscParticipantFirstNameAsc(Long tournamentId);

    boolean existsByTournamentIdAndParticipantId(Long tournamentId, Long participantId);

    boolean existsByParticipantId(Long participantId);

    void deleteByTournamentId(Long tournamentId);
}