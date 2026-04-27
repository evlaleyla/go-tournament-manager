package com.evlaleyla.gotournamentmanager.backend.macmahon;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for accessing persisted tournament standing entries.
 *
 * <p>This repository is used to load and remove imported wall-list based
 * standing data for a specific tournament.</p>
 */
public interface TournamentStandingRepository extends JpaRepository<TournamentStanding, Long> {

    /**
     * Returns all standing entries of a tournament ordered by ascending place.
     *
     * @param tournamentId the technical id of the tournament
     * @return all standing entries of the given tournament ordered by place
     */
    List<TournamentStanding> findByTournamentIdOrderByPlaceAsc(Long tournamentId);

    /**
     * Deletes all standing entries belonging to the given tournament.
     *
     * @param tournamentId the technical id of the tournament
     */
    void deleteByTournamentId(Long tournamentId);
}