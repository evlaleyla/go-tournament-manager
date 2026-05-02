package com.evlaleyla.gotournamentmanager.backend;

import com.evlaleyla.gotournamentmanager.backend.macmahon.TournamentStanding;
import com.evlaleyla.gotournamentmanager.backend.macmahon.TournamentStandingRepository;
import com.evlaleyla.gotournamentmanager.backend.pairing.Pairing;
import com.evlaleyla.gotournamentmanager.backend.pairing.PairingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Service for checking whether a participant name is already referenced
 * in imported tournament data such as pairings or wall-list standings.
 *
 * <p>This service is used to protect critical data changes, for example:
 * <ul>
 *     <li>preventing deletion of participants that are already referenced,</li>
 *     <li>preventing name changes after imported data has been stored.</li>
 * </ul>
 *
 * <p>Name comparison is performed on normalized values in order to make
 * matching more robust across different name notations.
 */
@Service
@Transactional(readOnly = true)
public class TournamentDataReferenceService {

    private static final Logger log = LoggerFactory.getLogger(TournamentDataReferenceService.class);

    private final PairingRepository pairingRepository;
    private final TournamentStandingRepository tournamentStandingRepository;

    public TournamentDataReferenceService(PairingRepository pairingRepository,
                                          TournamentStandingRepository tournamentStandingRepository) {
        this.pairingRepository = pairingRepository;
        this.tournamentStandingRepository = tournamentStandingRepository;
    }

    /**
     * Checks whether the given participant name is referenced in any imported pairing
     * of the specified tournament.
     *
     * @param tournamentId the tournament identifier
     * @param participantName the participant name to check
     * @return {@code true} if at least one matching pairing reference exists, otherwise {@code false}
     */
    public boolean hasPairingReferenceForTournamentAndParticipantName(Long tournamentId, String participantName) {
        String normalizedParticipantName = normalizeNameForLookup(participantName);

        if (tournamentId == null || normalizedParticipantName.isBlank()) {
            log.debug("Skipping pairing reference check because tournamentId or participantName is invalid.");
            return false;
        }

        boolean referenceExists = pairingRepository.findByTournamentIdOrderByRoundNumberAscTableNumberAsc(tournamentId)
                .stream()
                .anyMatch(pairing -> matchesPairing(pairing, normalizedParticipantName));

        log.debug(
                "Pairing reference check completed for tournamentId={} and participantName='{}': {}",
                tournamentId,
                normalizedParticipantName,
                referenceExists
        );

        return referenceExists;
    }

    /**
     * Checks whether the given participant name is referenced in any imported standing
     * of the specified tournament.
     *
     * @param tournamentId the tournament identifier
     * @param participantName the participant name to check
     * @return {@code true} if at least one matching standing reference exists, otherwise {@code false}
     */
    public boolean hasStandingReferenceForTournamentAndParticipantName(Long tournamentId, String participantName) {
        String normalizedParticipantName = normalizeNameForLookup(participantName);

        if (tournamentId == null || normalizedParticipantName.isBlank()) {
            log.debug("Skipping standing reference check because tournamentId or participantName is invalid.");
            return false;
        }

        boolean referenceExists = tournamentStandingRepository.findByTournamentIdOrderByPlaceAsc(tournamentId)
                .stream()
                .anyMatch(standing -> normalizedParticipantName.equals(normalizeNameForLookup(standing.getName())));

        log.debug(
                "Standing reference check completed for tournamentId={} and participantName='{}': {}",
                tournamentId,
                normalizedParticipantName,
                referenceExists
        );

        return referenceExists;
    }

    /**
     * Checks whether the given participant name is referenced in any imported pairing
     * across all tournaments.
     *
     * @param participantName the participant name to check
     * @return {@code true} if at least one matching pairing reference exists, otherwise {@code false}
     */
    public boolean hasPairingReferenceForParticipantName(String participantName) {
        String normalizedParticipantName = normalizeNameForLookup(participantName);

        if (normalizedParticipantName.isBlank()) {
            log.debug("Skipping global pairing reference check because participantName is blank.");
            return false;
        }

        boolean referenceExists = pairingRepository.findAll()
                .stream()
                .anyMatch(pairing -> matchesPairing(pairing, normalizedParticipantName));

        log.debug(
                "Global pairing reference check completed for participantName='{}': {}",
                normalizedParticipantName,
                referenceExists
        );

        return referenceExists;
    }

    /**
     * Checks whether the given participant name is referenced in any imported standing
     * across all tournaments.
     *
     * @param participantName the participant name to check
     * @return {@code true} if at least one matching standing reference exists, otherwise {@code false}
     */
    public boolean hasStandingReferenceForParticipantName(String participantName) {
        String normalizedParticipantName = normalizeNameForLookup(participantName);

        if (normalizedParticipantName.isBlank()) {
            log.debug("Skipping global standing reference check because participantName is blank.");
            return false;
        }

        boolean referenceExists = tournamentStandingRepository.findAll()
                .stream()
                .map(TournamentStanding::getName)
                .anyMatch(name -> normalizedParticipantName.equals(normalizeNameForLookup(name)));

        log.debug(
                "Global standing reference check completed for participantName='{}': {}",
                normalizedParticipantName,
                referenceExists
        );

        return referenceExists;
    }

    /**
     * Checks whether the given pairing references the normalized participant name
     * either as black player or white player.
     *
     * @param pairing the pairing to inspect
     * @param normalizedParticipantName the already normalized participant name
     * @return {@code true} if the pairing contains the participant name, otherwise {@code false}
     */
    private boolean matchesPairing(Pairing pairing, String normalizedParticipantName) {
        return normalizedParticipantName.equals(normalizeNameForLookup(pairing.getBlackPlayer()))
                || normalizedParticipantName.equals(normalizeNameForLookup(pairing.getWhitePlayer()));
    }

    /**
     * Normalizes a participant name for internal lookup operations.
     *
     * <p>Normalization rules:
     * <ul>
     *     <li>trim leading and trailing whitespace,</li>
     *     <li>collapse multiple spaces into a single space,</li>
     *     <li>convert names in "Last, First" format to "First Last",</li>
     *     <li>convert to lowercase using {@link Locale#ROOT}.</li>
     * </ul>
     *
     * @param value the raw name value
     * @return the normalized lookup value, or an empty string if the input is {@code null}
     */
    private String normalizeNameForLookup(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().replaceAll("\\s+", " ");

        // Convert names such as "Doe, Jane" to "Jane Doe"
        // so that imported MacMahon names and internal names can be matched consistently.
        if (normalized.contains(",")) {
            String[] parts = normalized.split(",", 2);
            String lastName = parts[0].trim();
            String firstName = parts[1].trim();
            normalized = (firstName + " " + lastName).trim();
        }

        return normalized.toLowerCase(Locale.ROOT);
    }
}