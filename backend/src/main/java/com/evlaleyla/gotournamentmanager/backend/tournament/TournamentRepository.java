package com.evlaleyla.gotournamentmanager.backend.tournament;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    List<Tournament> findByNameContainingIgnoreCase(String name);

    List<Tournament> findByStatus(TournamentStatus status);

    List<Tournament> findByNameContainingIgnoreCaseAndStatus(String name, TournamentStatus status);

    @Query("select distinct t.name from Tournament t order by t.name asc")
    List<String> findDistinctNames();
}