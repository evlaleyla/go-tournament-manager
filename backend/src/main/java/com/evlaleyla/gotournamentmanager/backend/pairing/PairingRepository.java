package com.evlaleyla.gotournamentmanager.backend.pairing;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PairingRepository extends JpaRepository<Pairing, Long> {

    List<Pairing> findByTournamentIdOrderByRoundNumberAscTableNumberAsc(Long tournamentId);

    List<Pairing> findByTournamentIdAndRoundNumberOrderByTableNumberAsc(Long tournamentId, Integer roundNumber);

    Optional<Pairing> findByTournamentIdAndRoundNumberAndTableNumber(Long tournamentId,
                                                                     Integer roundNumber,
                                                                     Integer tableNumber);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("delete from Pairing p where p.tournament.id = :tournamentId")
    void deleteByTournamentId(@Param("tournamentId") Long tournamentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
           delete from Pairing p
           where p.tournament.id = :tournamentId
             and p.roundNumber = :roundNumber
           """)
    void deleteByTournamentIdAndRoundNumber(@Param("tournamentId") Long tournamentId,
                                            @Param("roundNumber") Integer roundNumber);
}