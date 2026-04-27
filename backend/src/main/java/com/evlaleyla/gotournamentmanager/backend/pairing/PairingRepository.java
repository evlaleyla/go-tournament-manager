package com.evlaleyla.gotournamentmanager.backend.pairing;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing and modifying pairing data.
 *
 * <p>This repository provides query methods for loading pairings by tournament,
 * round, publication status, and table number. It also contains specialized
 * delete and aggregation queries used by the service layer.</p>
 */
public interface PairingRepository extends JpaRepository<Pairing, Long> {

    /**
     * Returns all pairings of a tournament ordered by round number and table number.
     *
     * @param tournamentId the tournament ID
     * @return all pairings of the tournament in deterministic display order
     */
    List<Pairing> findByTournamentIdOrderByRoundNumberAscTableNumberAsc(Long tournamentId);

    /**
     * Returns all pairings of a specific tournament round ordered by table number.
     *
     * @param tournamentId the tournament ID
     * @param roundNumber  the round number
     * @return all pairings of the requested round
     */
    List<Pairing> findByTournamentIdAndRoundNumberOrderByTableNumberAsc(Long tournamentId, Integer roundNumber);

    /**
     * Returns only published pairings of a specific tournament round ordered by table number.
     *
     * @param tournamentId the tournament ID
     * @param roundNumber  the round number
     * @return published pairings of the requested round
     */
    List<Pairing> findByTournamentIdAndRoundNumberAndPublishedTrueOrderByTableNumberAsc(Long tournamentId,
                                                                                        Integer roundNumber);

    /**
     * Finds a pairing uniquely by tournament, round, and table number.
     *
     * @param tournamentId the tournament ID
     * @param roundNumber  the round number
     * @param tableNumber  the table number
     * @return the matching pairing if present
     */
    Optional<Pairing> findByTournamentIdAndRoundNumberAndTableNumber(Long tournamentId,
                                                                     Integer roundNumber,
                                                                     Integer tableNumber);

    /**
     * Checks whether at least one published pairing exists for the given round.
     *
     * @param tournamentId the tournament ID
     * @param roundNumber  the round number
     * @return {@code true} if published pairings exist, otherwise {@code false}
     */
    boolean existsByTournamentIdAndRoundNumberAndPublishedTrue(Long tournamentId, Integer roundNumber);

    /**
     * Deletes all pairings belonging to a tournament.
     *
     * <p>This is used, for example, when a tournament is removed completely.</p>
     *
     * @param tournamentId the tournament ID
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("delete from Pairing p where p.tournament.id = :tournamentId")
    void deleteByTournamentId(@Param("tournamentId") Long tournamentId);

    /**
     * Deletes all pairings of a specific tournament round.
     *
     * <p>This is typically used before importing a fresh pairing set for the same round.</p>
     *
     * @param tournamentId the tournament ID
     * @param roundNumber  the round number to delete
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
           delete from Pairing p
           where p.tournament.id = :tournamentId
             and p.roundNumber = :roundNumber
           """)
    void deleteByTournamentIdAndRoundNumber(@Param("tournamentId") Long tournamentId,
                                            @Param("roundNumber") Integer roundNumber);

    /**
     * Counts how many distinct rounds already have imported pairings.
     *
     * @param tournamentId the tournament ID
     * @return number of rounds for which pairings exist
     */
    @Query("""
       select count(distinct p.roundNumber)
       from Pairing p
       where p.tournament.id = :tournamentId
       """)
    long countImportedRoundsByTournamentId(@Param("tournamentId") Long tournamentId);

    /**
     * Returns the highest round number for which pairings currently exist.
     *
     * @param tournamentId the tournament ID
     * @return the highest imported round number, or {@code null} if none exist
     */
    @Query("""
       select max(p.roundNumber)
       from Pairing p
       where p.tournament.id = :tournamentId
       """)
    Integer findLastImportedRoundByTournamentId(@Param("tournamentId") Long tournamentId);
}