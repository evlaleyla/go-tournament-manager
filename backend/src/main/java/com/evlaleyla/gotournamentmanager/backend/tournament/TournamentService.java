package com.evlaleyla.gotournamentmanager.backend.tournament;

import com.evlaleyla.gotournamentmanager.backend.macmahon.TournamentStanding;
import com.evlaleyla.gotournamentmanager.backend.macmahon.TournamentStandingRepository;
import com.evlaleyla.gotournamentmanager.backend.pairing.Pairing;
import com.evlaleyla.gotournamentmanager.backend.pairing.PairingRepository;
import com.evlaleyla.gotournamentmanager.backend.registration.Registration;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final RegistrationRepository registrationRepository;
    private final PairingRepository pairingRepository;
    private final TournamentStandingRepository tournamentStandingRepository;

    public TournamentService(TournamentRepository tournamentRepository,
                             RegistrationRepository registrationRepository,
                             PairingRepository pairingRepository,
                             TournamentStandingRepository tournamentStandingRepository) {
        this.tournamentRepository = tournamentRepository;
        this.registrationRepository = registrationRepository;
        this.pairingRepository = pairingRepository;
        this.tournamentStandingRepository = tournamentStandingRepository;
    }

    public List<Tournament> findAll() {
        return tournamentRepository.findAll();
    }

    public Tournament save(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    public Tournament findById(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Turnier nicht gefunden: " + id));
    }

    public Tournament update(Long id, Tournament updatedTournament) {
        Tournament existingTournament = findById(id);

        validateRoundCountReduction(existingTournament, updatedTournament.getNumberOfRounds());

        existingTournament.setName(updatedTournament.getName());
        existingTournament.setLocation(updatedTournament.getLocation());
        existingTournament.setStartDate(updatedTournament.getStartDate());
        existingTournament.setEndDate(updatedTournament.getEndDate());
        existingTournament.setRegistrationDeadline(updatedTournament.getRegistrationDeadline());
        existingTournament.setNumberOfRounds(updatedTournament.getNumberOfRounds());
        existingTournament.setDescription(updatedTournament.getDescription());
        existingTournament.setStatus(updatedTournament.getStatus());

        return tournamentRepository.save(existingTournament);
    }

    public List<Tournament> search(String search, TournamentStatus status) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasStatus = status != null;

        if (hasSearch && hasStatus) {
            return tournamentRepository.findByNameContainingIgnoreCaseAndStatus(search, status);
        }

        if (hasSearch) {
            return tournamentRepository.findByNameContainingIgnoreCase(search);
        }

        if (hasStatus) {
            return tournamentRepository.findByStatus(status);
        }

        return tournamentRepository.findAll();
    }

    public List<String> findDistinctNames() {
        return tournamentRepository.findDistinctNames();
    }

    @Transactional
    public void deleteById(Long id) {
        pairingRepository.deleteByTournamentId(id);
        registrationRepository.deleteByTournamentId(id);
        tournamentStandingRepository.deleteByTournamentId(id);
        tournamentRepository.deleteById(id);
    }

    private void validateRoundCountReduction(Tournament existingTournament, Integer newRoundCount) {
        Integer currentRoundCount = existingTournament.getNumberOfRounds();

        if (newRoundCount == null || currentRoundCount == null) {
            return;
        }

        // Erhöhung oder gleiche Rundenzahl ist immer erlaubt
        if (newRoundCount >= currentRoundCount) {
            return;
        }

        Long tournamentId = existingTournament.getId();

        int highestRegistrationRound = findHighestRoundUsedInRegistrations(tournamentId);
        int highestPairingRound = findHighestRoundUsedInPairings(tournamentId);
        int highestWallListRound = findHighestRoundUsedInWallList(tournamentId);

        List<String> blockingReasons = new ArrayList<>();

        if (highestRegistrationRound > newRoundCount) {
            blockingReasons.add("Anmeldungen bis Runde " + highestRegistrationRound);
        }

        if (highestPairingRound > newRoundCount) {
            blockingReasons.add("Paarungen bis Runde " + highestPairingRound);
        }

        if (highestWallListRound > newRoundCount) {
            blockingReasons.add("Wall-List-Daten mit " + highestWallListRound + " Runden");
        }

        if (!blockingReasons.isEmpty()) {
            throw new IllegalArgumentException(
                    "Die Rundenzahl kann nicht auf " + newRoundCount +
                            " reduziert werden, da bereits " +
                            String.join(", ", blockingReasons) +
                            " existieren."
            );
        }
    }

    private int findHighestRoundUsedInRegistrations(Long tournamentId) {
        return registrationRepository.findByTournamentIdOrderByRegistrationDateAsc(tournamentId).stream()
                .map(Registration::getSelectedRounds)
                .flatMap(java.util.Set::stream)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private int findHighestRoundUsedInPairings(Long tournamentId) {
        return pairingRepository.findByTournamentIdOrderByRoundNumberAscTableNumberAsc(tournamentId).stream()
                .map(Pairing::getRoundNumber)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private int findHighestRoundUsedInWallList(Long tournamentId) {
        return tournamentStandingRepository.findByTournamentIdOrderByPlaceAsc(tournamentId).stream()
                .map(TournamentStanding::getRoundStatuses)
                .mapToInt(List::size)
                .max()
                .orElse(0);
    }
}