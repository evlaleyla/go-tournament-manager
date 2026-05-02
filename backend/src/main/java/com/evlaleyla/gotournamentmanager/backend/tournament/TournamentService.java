package com.evlaleyla.gotournamentmanager.backend.tournament;

import com.evlaleyla.gotournamentmanager.backend.macmahon.TournamentStanding;
import com.evlaleyla.gotournamentmanager.backend.macmahon.TournamentStandingRepository;
import com.evlaleyla.gotournamentmanager.backend.pairing.Pairing;
import com.evlaleyla.gotournamentmanager.backend.pairing.PairingRepository;
import com.evlaleyla.gotournamentmanager.backend.registration.Registration;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for managing tournament entities and related
 * business rules.
 *
 * <p>This service centralizes tournament persistence, search operations,
 * update rules, and cascading cleanup of dependent tournament data.</p>
 */
@Service
public class TournamentService {

    private static final Logger log = LoggerFactory.getLogger(TournamentService.class);

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

    /**
     * Returns all tournaments.
     *
     * @return list of all stored tournaments
     */
    public List<Tournament> findAll() {
        log.debug("Fetching all tournaments.");
        return tournamentRepository.findAll();
    }

    /**
     * Persists a new tournament or saves changes to an existing one.
     *
     * @param tournament the tournament to save
     * @return the persisted tournament
     */
    public Tournament save(Tournament tournament) {
        log.info("Saving tournament. name='{}', id={}", tournament.getName(), tournament.getId());

        Tournament savedTournament = tournamentRepository.save(tournament);

        log.info("Tournament saved successfully. id={}, name='{}'",
                savedTournament.getId(),
                savedTournament.getName());

        return savedTournament;
    }

    /**
     * Finds a tournament by its identifier.
     *
     * @param id the tournament id
     * @return the matching tournament
     * @throws IllegalArgumentException if no tournament exists for the given id
     */
    public Tournament findById(Long id) {
        log.debug("Fetching tournament by id={}.", id);

        return tournamentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tournament not found. id={}", id);
                    return new IllegalArgumentException("Turnier nicht gefunden: " + id);
                });
    }

    /**
     * Updates an existing tournament.
     *
     * <p>Before applying the changes, the service validates whether a reduction
     * of the configured round count would conflict with already existing
     * registrations, pairings, or imported wall-list data.</p>
     *
     * @param id the id of the tournament to update
     * @param updatedTournament the new tournament state
     * @return the updated and persisted tournament
     */
    public Tournament update(Long id, Tournament updatedTournament) {
        log.info("Updating tournament. id={}", id);

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

        Tournament savedTournament = tournamentRepository.save(existingTournament);

        log.info("Tournament updated successfully. id={}, name='{}'",
                savedTournament.getId(),
                savedTournament.getName());

        return savedTournament;
    }

    /**
     * Searches tournaments by optional search term and/or status.
     *
     * @param search optional search term for the tournament name
     * @param status optional tournament status filter
     * @return list of matching tournaments
     */
    public List<Tournament> search(String search, TournamentStatus status) {
        String normalizedSearch = search == null ? null : search.trim();

        boolean hasSearch = normalizedSearch != null && !normalizedSearch.isBlank();
        boolean hasStatus = status != null;

        log.debug("Searching tournaments. search='{}', status={}", normalizedSearch, status);

        if (hasSearch && hasStatus) {
            return tournamentRepository.findByNameContainingIgnoreCaseAndStatus(normalizedSearch, status);
        }

        if (hasSearch) {
            return tournamentRepository.findByNameContainingIgnoreCase(normalizedSearch);
        }

        if (hasStatus) {
            return tournamentRepository.findByStatus(status);
        }

        return tournamentRepository.findAll();
    }

    /**
     * Returns all distinct tournament names sorted alphabetically.
     *
     * @return list of distinct tournament names
     */
    public List<String> findDistinctNames() {
        log.debug("Fetching distinct tournament names.");
        return tournamentRepository.findDistinctNames();
    }

    /**
     * Deletes a tournament and all dependent tournament data in a single transaction.
     *
     * <p>The deletion order is important to avoid referential integrity problems:
     * pairings, registrations, and standings are removed before the tournament
     * entity itself is deleted.</p>
     *
     * @param id the id of the tournament to delete
     */
    @Transactional
    public void deleteById(Long id) {
        Tournament tournament = findById(id);

        log.info("Deleting tournament and related data. id={}, name='{}'",
                tournament.getId(),
                tournament.getName());

        pairingRepository.deleteByTournamentId(id);
        registrationRepository.deleteByTournamentId(id);
        tournamentStandingRepository.deleteByTournamentId(id);
        tournamentRepository.deleteById(id);

        log.info("Tournament deleted successfully. id={}, name='{}'",
                tournament.getId(),
                tournament.getName());
    }

    /**
     * Validates whether reducing the number of rounds is still compatible with
     * already persisted tournament data.
     *
     * <p>A reduction is blocked if registrations, pairings, or imported wall-list
     * data already reference rounds that would become invalid.</p>
     *
     * @param existingTournament the current persisted tournament
     * @param newRoundCount the requested new round count
     */
    private void validateRoundCountReduction(Tournament existingTournament, Integer newRoundCount) {
        Integer currentRoundCount = existingTournament.getNumberOfRounds();

        if (newRoundCount == null || currentRoundCount == null) {
            log.debug("Skipping round count reduction validation because round count is null.");
            return;
        }

        // Increasing the number of rounds or keeping it unchanged is always allowed.
        if (newRoundCount >= currentRoundCount) {
            log.debug("Round count reduction validation not required. currentRoundCount={}, newRoundCount={}",
                    currentRoundCount,
                    newRoundCount);
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
            log.warn(
                    "Round count reduction blocked for tournament id={}. requestedRoundCount={}, reasons={}",
                    tournamentId,
                    newRoundCount,
                    blockingReasons
            );

            throw new IllegalArgumentException(
                    "Die Rundenzahl kann nicht auf " + newRoundCount +
                            " reduziert werden, da bereits " +
                            String.join(", ", blockingReasons) +
                            " existieren."
            );
        }

        log.debug("Round count reduction validated successfully for tournament id={}. newRoundCount={}",
                tournamentId,
                newRoundCount);
    }

    /**
     * Determines the highest round number currently referenced by registrations.
     *
     * @param tournamentId the tournament id
     * @return the highest referenced round number, or 0 if none exists
     */
    private int findHighestRoundUsedInRegistrations(Long tournamentId) {
        int highestRound = registrationRepository.findByTournamentIdOrderByRegistrationDateAsc(tournamentId).stream()
                .map(Registration::getSelectedRounds)
                .flatMap(java.util.Set::stream)
                .max(Integer::compareTo)
                .orElse(0);

        log.debug("Highest registration round for tournament id={} is {}.", tournamentId, highestRound);
        return highestRound;
    }

    /**
     * Determines the highest round number currently referenced by pairings.
     *
     * @param tournamentId the tournament id
     * @return the highest referenced round number, or 0 if none exists
     */
    private int findHighestRoundUsedInPairings(Long tournamentId) {
        int highestRound = pairingRepository.findByTournamentIdOrderByRoundNumberAscTableNumberAsc(tournamentId).stream()
                .map(Pairing::getRoundNumber)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        log.debug("Highest pairing round for tournament id={} is {}.", tournamentId, highestRound);
        return highestRound;
    }

    /**
     * Determines the highest round count currently represented in imported
     * wall-list data.
     *
     * <p>This is derived from the number of stored round status entries per
     * standing row.</p>
     *
     * @param tournamentId the tournament id
     * @return the highest referenced wall-list round count, or 0 if none exists
     */
    private int findHighestRoundUsedInWallList(Long tournamentId) {
        int highestRound = tournamentStandingRepository.findByTournamentIdOrderByPlaceAsc(tournamentId).stream()
                .map(TournamentStanding::getRoundStatuses)
                .mapToInt(List::size)
                .max()
                .orElse(0);

        log.debug("Highest wall-list round count for tournament id={} is {}.", tournamentId, highestRound);
        return highestRound;
    }
}