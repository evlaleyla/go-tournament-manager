package com.evlaleyla.gotournamentmanager.backend.pairing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PairingRepository extends JpaRepository<Pairing, Long> {

    List<Pairing> findByTournamentIdOrderByRoundNumberAscTableNumberAsc(Long tournamentId);

    Optional<Pairing> findByTournamentIdAndRoundNumberAndTableNumber(Long tournamentId,
                                                                     Integer roundNumber,
                                                                     Integer tableNumber);

    void deleteByTournamentId(Long tournamentId);
}