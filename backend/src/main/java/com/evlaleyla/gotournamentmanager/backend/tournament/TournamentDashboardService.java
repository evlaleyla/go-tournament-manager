package com.evlaleyla.gotournamentmanager.backend.tournament;

import com.evlaleyla.gotournamentmanager.backend.pairing.PairingRepository;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationRepository;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationService;
import org.springframework.stereotype.Service;

@Service
public class TournamentDashboardService {

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

    public TournamentDashboardData buildDashboard(Tournament tournament) {
        Long tournamentId = tournament.getId();

        long totalRegistrations = registrationRepository.countByTournamentId(tournamentId);
        long eligibleInRoundOne = registrationService.findStartListByTournamentIdAndRound(tournamentId, 1).size();

        long importedPairingRoundCount = pairingRepository.countImportedRoundsByTournamentId(tournamentId);
        Integer lastImportedPairingRound = pairingRepository.findLastImportedRoundByTournamentId(tournamentId);

        int currentRound = determineCurrentRound(tournament.getNumberOfRounds(), lastImportedPairingRound);
        long eligibleInCurrentRound =
                registrationService.findStartListByTournamentIdAndRound(tournamentId, currentRound).size();

        return new TournamentDashboardData(
                totalRegistrations,
                eligibleInRoundOne,
                currentRound,
                eligibleInCurrentRound,
                importedPairingRoundCount,
                lastImportedPairingRound,
                tournament.getLastWallListImportAt()
        );
    }

    private int determineCurrentRound(Integer numberOfRounds, Integer lastImportedPairingRound) {
        if (numberOfRounds == null || numberOfRounds < 1) {
            return 1;
        }

        if (lastImportedPairingRound == null || lastImportedPairingRound < 1) {
            return 1;
        }

        return Math.min(lastImportedPairingRound, numberOfRounds);
    }
}