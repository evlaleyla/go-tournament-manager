package com.evlaleyla.gotournamentmanager.backend.tournament;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository for accessing and querying {@link Tournament} entities.
 *
 * <p>This repository provides search methods used by the administrative
 * tournament overview and related UI components.</p>
 */
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    /**
     * Finds all tournaments whose name contains the given search term,
     * ignoring letter case.
     *
     * @param name the search term to match against the tournament name
     * @return all matching tournaments
     */
    List<Tournament> findByNameContainingIgnoreCase(String name);

    /**
     * Finds all tournaments with the given status.
     *
     * @param status the tournament status to filter by
     * @return all tournaments with the given status
     */
    List<Tournament> findByStatus(TournamentStatus status);

    /**
     * Finds all tournaments whose name contains the given search term
     * and that match the given status.
     *
     * @param name the search term to match against the tournament name
     * @param status the tournament status to filter by
     * @return all tournaments matching both criteria
     */
    List<Tournament> findByNameContainingIgnoreCaseAndStatus(String name, TournamentStatus status);

    /**
     * Returns all distinct tournament names in ascending alphabetical order.
     *
     * <p>This method is used, for example, to populate suggestion lists
     * for name-based search fields.</p>
     *
     * @return sorted list of distinct tournament names
     */
    @Query("select distinct t.name from Tournament t order by t.name asc")
    List<String> findDistinctNames();
}