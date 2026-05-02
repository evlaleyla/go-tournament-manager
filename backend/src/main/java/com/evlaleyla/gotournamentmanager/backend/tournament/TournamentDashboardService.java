package com.evlaleyla.gotournamentmanager.backend.tournament;

import com.evlaleyla.gotournamentmanager.backend.pairing.PairingRepository;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationRepository;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for building aggregated dashboard data for a tournament.
 *
 * <p>The dashboard combines registration-related data, pairing import status,
 * and wall list import information so that the tournament detail page can
 * present a compact overview of the current organizational state.</p>
 */
@Service
public class TournamentDashboardService {

    private static final Logger log = LoggerFactory.getLogger(TournamentDashboardService.class);

    /**
     * Default fallback round used when no valid round information is available.
     */
    private static final int DEFAULT_ROUND = 1;

    private final RegistrationRepository registrationRepository;
    private final RegistrationService registrationService;
    private final PairingRepository pairingRepository;

    public TournamentDashboardService(RegistrationRepository registrationRepository,
                                      RegistrationService registrationService,
                                      PairingRepository pairingRepository) {
        this.registrationRepository = registrationRepository;
        this.registrationService = registrationService;
        this.pairingRepository = pairingRepository;
    }

    /**
     * Builds dashboard data for the given tournament.
     *
     * <p>The resulting object contains aggregated values such as the number
     * of registrations, eligible participants for relevant rounds, and pairing
     * import metadata.</p>
     *
     * @param tournament the tournament for which dashboard data should be created
     * @return aggregated dashboard data for the given tournament
     * @throws IllegalArgumentException if the tournament or its ID is missing
     */
    public TournamentDashboardData buildDashboard(Tournament tournament) {
        validateTournament(tournament);

        Long tournamentId = tournament.getId();

        log.debug(
                "Building dashboard data for tournamentId={}, tournamentName='{}'.",
                tournamentId,
                tournament.getName()
        );

        long totalRegistrations = registrationRepository.countByTournamentId(tournamentId);
        long eligibleInRoundOne = countEligibleParticipantsForRound(tournamentId, DEFAULT_ROUND);

        long importedPairingRoundCount = pairingRepository.countImportedRoundsByTournamentId(tournamentId);
        Integer lastImportedPairingRound = pairingRepository.findLastImportedRoundByTournamentId(tournamentId);

        int currentRound = determineCurrentRound(
                tournament.getNumberOfRounds(),
                lastImportedPairingRound
        );

        long eligibleInCurrentRound = countEligibleParticipantsForRound(tournamentId, currentRound);

        TournamentDashboardData dashboardData = new TournamentDashboardData(
                totalRegistrations,
                eligibleInRoundOne,
                currentRound,
                eligibleInCurrentRound,
                importedPairingRoundCount,
                lastImportedPairingRound,
                tournament.getLastWallListImportAt()
        );

        log.debug(
                "Dashboard data built successfully for tournamentId={}: totalRegistrations={}, " +
                        "eligibleInRoundOne={}, currentRound={}, eligibleInCurrentRound={}, " +
                        "importedPairingRoundCount={}, lastImportedPairingRound={}.",
                tournamentId,
                totalRegistrations,
                eligibleInRoundOne,
                currentRound,
                eligibleInCurrentRound,
                importedPairingRoundCount,
                lastImportedPairingRound
        );

        return dashboardData;
    }

    /**
     * Counts how many registered participants are eligible to play in a given round.
     *
     * @param tournamentId the tournament ID
     * @param roundNumber  the round number
     * @return number of eligible participants in the given round
     */
    private long countEligibleParticipantsForRound(Long tournamentId, int roundNumber) {
        return registrationService.findStartListByTournamentIdAndRound(tournamentId, roundNumber).size();
    }

    /**
     * Determines which round should be shown as the current round in the dashboard.
     *
     * <p>If no valid round information is available, round {@value #DEFAULT_ROUND}
     * is used as a safe fallback.</p>
     *
     * @param numberOfRounds          total configured number of rounds of the tournament
     * @param lastImportedPairingRound highest round for which pairings were imported
     * @return the round number that should be shown in the dashboard
     */
    private int determineCurrentRound(Integer numberOfRounds, Integer lastImportedPairingRound) {
        if (numberOfRounds == null || numberOfRounds < 1) {
            log.debug(
                    "Tournament has no valid number of rounds configured. Falling back to default round {}.",
                    DEFAULT_ROUND
            );
            return DEFAULT_ROUND;
        }

        if (lastImportedPairingRound == null || lastImportedPairingRound < 1) {
            log.debug(
                    "No valid imported pairing round found. Falling back to default round {}.",
                    DEFAULT_ROUND
            );
            return DEFAULT_ROUND;
        }

        return Math.min(lastImportedPairingRound, numberOfRounds);
    }

    /**
     * Validates the tournament argument before dashboard data is created.
     *
     * @param tournament the tournament to validate
     * @throws IllegalArgumentException if the tournament or its ID is missing
     */
    private void validateTournament(Tournament tournament) {
        if (tournament == null) {
            log.warn("Cannot build dashboard data because the tournament argument is null.");
            throw new IllegalArgumentException("Tournament must not be null.");
        }

        if (tournament.getId() == null) {
            log.warn("Cannot build dashboard data because the tournament ID is null.");
            throw new IllegalArgumentException("Tournament ID must not be null.");
        }
    }
}