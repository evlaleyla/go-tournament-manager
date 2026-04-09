package com.evlaleyla.gotournamentmanager.backend.tournament;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    List<Tournament> findByNameContainingIgnoreCaseOrderByStartDateAsc(String name);

    List<Tournament> findByStatusOrderByStartDateAsc(TournamentStatus status);

    List<Tournament> findByNameContainingIgnoreCaseAndStatusOrderByStartDateAsc(String name, TournamentStatus status);

    @Query("select distinct t.name from Tournament t order by t.name asc")
    List<String> findDistinctNames();
}