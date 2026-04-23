package com.evlaleyla.gotournamentmanager.backend.macmahon;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentStandingRepository extends JpaRepository<TournamentStanding, Long> {

    List<TournamentStanding> findByTournamentIdOrderByPlaceAsc(Long tournamentId);

    void deleteByTournamentId(Long tournamentId);
}