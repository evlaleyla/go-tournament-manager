package com.evlaleyla.gotournamentmanager.backend.pairing;

import com.evlaleyla.gotournamentmanager.backend.macmahon.MacMahonInterfaceService;
import com.evlaleyla.gotournamentmanager.backend.macmahon.MacMahonPairingImportRow;
import com.evlaleyla.gotournamentmanager.backend.registration.Registration;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationService;
import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Service for managing tournament pairings.
 *
 * <p>This service is responsible for retrieving pairings, updating results,
 * publishing and unpublishing rounds, and importing pairing data from
 * MacMahon export files.</p>
 */
@Service
public class PairingService {

    private static final Logger log = LoggerFactory.getLogger(PairingService.class);

    /**
     * Internal result codes supported by the application.
     */
    private static final Set<String> VALID_RESULT_CODES = Set.of("B", "W", "J", "L", "D");

    private final PairingRepository pairingRepository;
    private final TournamentRepository tournamentRepository;
    private final MacMahonInterfaceService macMahonInterfaceService;
    private final RegistrationService registrationService;

    public PairingService(PairingRepository pairingRepository,
                          TournamentRepository tournamentRepository,
                          MacMahonInterfaceService macMahonInterfaceService,
                          RegistrationService registrationService) {
        this.pairingRepository = pairingRepository;
        this.tournamentRepository = tournamentRepository;
        this.macMahonInterfaceService = macMahonInterfaceService;
        this.registrationService = registrationService;
    }

    /**
     * Returns all pairings of a tournament ordered by round number and table number.
     *
     * @param tournamentId the tournament ID
     * @return ordered list of pairings
     */
    public List<Pairing> findByTournamentId(Long tournamentId) {
        log.debug("Loading all pairings for tournamentId={}", tournamentId);
        return pairingRepository.findByTournamentIdOrderByRoundNumberAscTableNumberAsc(tournamentId);
    }

    /**
     * Returns all pairings of a specific round ordered by table number.
     *
     * @param tournamentId the tournament ID
     * @param roundNumber the round number
     * @return ordered list of pairings for the given round
     */
    public List<Pairing> findByTournamentIdAndRound(Long tournamentId, Integer roundNumber) {
        log.debug("Loading pairings for tournamentId={} and roundNumber={}", tournamentId, roundNumber);
        return pairingRepository.findByTournamentIdAndRoundNumberOrderByTableNumberAsc(tournamentId, roundNumber);
    }

    /**
     * Returns all published pairings of a specific round ordered by table number.
     *
     * @param tournamentId the tournament ID
     * @param roundNumber the round number
     * @return ordered list of published pairings
     */
    public List<Pairing> findPublishedByTournamentIdAndRound(Long tournamentId, Integer roundNumber) {
        log.debug("Loading published pairings for tournamentId={} and roundNumber={}", tournamentId, roundNumber);
        return pairingRepository.findByTournamentIdAndRoundNumberAndPublishedTrueOrderByTableNumberAsc(
                tournamentId,
                roundNumber
        );
    }

    /**
     * Returns all round numbers that currently contain at least one published pairing.
     *
     * @param tournamentId the tournament ID
     * @return sorted set of published round numbers
     */
    public Set<Integer> findPublishedRoundNumbers(Long tournamentId) {
        log.debug("Loading published round numbers for tournamentId={}", tournamentId);

        return pairingRepository.findByTournamentIdOrderByRoundNumberAscTableNumberAsc(tournamentId)
                .stream()
                .filter(Pairing::isPublished)
                .map(Pairing::getRoundNumber)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns a pairing by ID.
     *
     * @param id the pairing ID
     * @return the pairing entity
     * @throws IllegalArgumentException if no pairing exists for the given ID
     */
    public Pairing findById(Long id) {
        log.debug("Loading pairing by id={}", id);

        return pairingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paarung nicht gefunden: " + id));
    }

    /**
     * Updates the result of an existing pairing.
     *
     * @param pairingId the pairing ID
     * @param result the internal result code or {@code null} for an open result
     * @return the updated pairing
     */
    public Pairing updateResult(Long pairingId, String result) {
        log.info("Updating result for pairingId={}", pairingId);

        Pairing pairing = findById(pairingId);
        String normalizedResult = normalizeAndValidateSimpleResult(result);

        pairing.setResult(normalizedResult);

        Pairing savedPairing = pairingRepository.save(pairing);

        log.info(
                "Successfully updated result for pairingId={} to result={}",
                pairingId,
                normalizedResult == null ? "OPEN" : normalizedResult
        );

        return savedPairing;
    }

    /**
     * Publishes all pairings of a given round.
     *
     * @param tournamentId the tournament ID
     * @param roundNumber the round number to publish
     */
    @Transactional
    public void publishRound(Long tournamentId, Integer roundNumber) {
        log.info("Publishing round {} for tournamentId={}", roundNumber, tournamentId);

        Tournament tournament = findTournamentOrThrow(tournamentId);
        validateRoundNumber(tournament, roundNumber);

        List<Pairing> roundPairings = pairingRepository
                .findByTournamentIdAndRoundNumberOrderByTableNumberAsc(tournamentId, roundNumber);

        if (roundPairings.isEmpty()) {
            throw new IllegalArgumentException(
                    "Für Runde " + roundNumber + " gibt es keine importierten Paarungen zum Veröffentlichen."
            );
        }

        for (Pairing pairing : roundPairings) {
            pairing.setPublished(true);
        }

        pairingRepository.saveAll(roundPairings);
        pairingRepository.flush();

        log.info(
                "Successfully published {} pairings for tournamentId={} and roundNumber={}",
                roundPairings.size(),
                tournamentId,
                roundNumber
        );
    }

    /**
     * Removes the publication status from all pairings of a given round.
     *
     * @param tournamentId the tournament ID
     * @param roundNumber the round number to unpublish
     */
    @Transactional
    public void unpublishRound(Long tournamentId, Integer roundNumber) {
        log.info("Unpublishing round {} for tournamentId={}", roundNumber, tournamentId);

        Tournament tournament = findTournamentOrThrow(tournamentId);
        validateRoundNumber(tournament, roundNumber);

        List<Pairing> roundPairings = pairingRepository
                .findByTournamentIdAndRoundNumberOrderByTableNumberAsc(tournamentId, roundNumber);

        if (roundPairings.isEmpty()) {
            throw new IllegalArgumentException(
                    "Für Runde " + roundNumber + " gibt es keine importierten Paarungen."
            );
        }

        for (Pairing pairing : roundPairings) {
            pairing.setPublished(false);
        }

        pairingRepository.saveAll(roundPairings);
        pairingRepository.flush();

        log.info(
                "Successfully unpublished {} pairings for tournamentId={} and roundNumber={}",
                roundPairings.size(),
                tournamentId,
                roundNumber
        );
    }

    /**
     * Imports pairings for a specific round from a MacMahon export file.
     *
     * <p>The import performs several validation steps before data is persisted:
     * round validation, duplicate table checks, round consistency checks, and
     * participant validation against tournament registrations.</p>
     *
     * @param tournamentId the tournament ID
     * @param roundNumber the selected round number
     * @param file the uploaded MacMahon export file
     */
    @Transactional
    public void importPairingsFromMacMahon(Long tournamentId, Integer roundNumber, MultipartFile file) {
        log.info(
                "Starting MacMahon pairing import for tournamentId={} and roundNumber={}",
                tournamentId,
                roundNumber
        );

        Tournament tournament = findTournamentOrThrow(tournamentId);
        validateRoundNumber(tournament, roundNumber);

        List<MacMahonPairingImportRow> importedRows =
                macMahonInterfaceService.parsePairingsExport(file, roundNumber);

        log.debug(
                "Parsed {} pairing rows from MacMahon file for tournamentId={} and roundNumber={}",
                importedRows.size(),
                tournamentId,
                roundNumber
        );

        if (importedRows.isEmpty()) {
            throw new IllegalArgumentException("Die Datei enthält keine Paarungen.");
        }

        validateUniqueTableNumbersFromRows(importedRows);

        for (MacMahonPairingImportRow imported : importedRows) {
            if (!roundNumber.equals(imported.roundNumber())) {
                throw new IllegalArgumentException(
                        "Die importierte Datei enthält Paarungen für eine andere Runde als die ausgewählte Runde "
                                + roundNumber + "."
                );
            }
        }

        validateImportedPlayersAgainstRegistrations(tournamentId, roundNumber, importedRows);

        log.debug(
                "Deleting existing pairings for tournamentId={} and roundNumber={} before import",
                tournamentId,
                roundNumber
        );

        pairingRepository.deleteByTournamentIdAndRoundNumber(tournamentId, roundNumber);
        pairingRepository.flush();

        List<Pairing> entitiesToSave = importedRows.stream()
                .map(imported -> mapImportRowToPairing(tournament, imported))
                .toList();

        pairingRepository.saveAll(entitiesToSave);
        pairingRepository.flush();

        log.info(
                "Successfully imported {} pairings for tournamentId={} and roundNumber={}",
                entitiesToSave.size(),
                tournamentId,
                roundNumber
        );
    }

    /**
     * Loads a tournament by ID or throws a business exception if it does not exist.
     *
     * @param tournamentId the tournament ID
     * @return the tournament entity
     */
    private Tournament findTournamentOrThrow(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Turnier nicht gefunden: " + tournamentId));
    }

    /**
     * Validates whether the given round number is defined for the tournament.
     *
     * @param tournament the tournament to validate against
     * @param roundNumber the requested round number
     */
    private void validateRoundNumber(Tournament tournament, Integer roundNumber) {
        if (roundNumber == null) {
            throw new IllegalArgumentException("Bitte eine Runde auswählen.");
        }

        if (tournament.getNumberOfRounds() == null || tournament.getNumberOfRounds() < 1) {
            throw new IllegalArgumentException("Für dieses Turnier ist keine gültige Rundenzahl definiert.");
        }

        if (roundNumber < 1 || roundNumber > tournament.getNumberOfRounds()) {
            throw new IllegalArgumentException(
                    "Die Runde " + roundNumber + " liegt außerhalb der definierten Turnierrunden."
            );
        }
    }

    /**
     * Normalizes and validates a simple internal result code.
     *
     * <p>Allowed values are:
     * {@code B}, {@code W}, {@code J}, {@code L}, {@code D}, or {@code null} / blank.</p>
     *
     * @param result the raw result value
     * @return the normalized result code or {@code null}
     */
    private String normalizeAndValidateSimpleResult(String result) {
        if (result == null || result.isBlank()) {
            return null;
        }

        String normalized = result.trim().toUpperCase(Locale.ROOT);

        if (!VALID_RESULT_CODES.contains(normalized)) {
            throw new IllegalArgumentException("Ungültiges Ergebnis. Erlaubt sind B, W, J, L, D oder leer.");
        }

        return normalized;
    }

    /**
     * Ensures that each imported row uses a unique table number within the round.
     *
     * @param rows imported pairing rows
     */
    private void validateUniqueTableNumbersFromRows(List<MacMahonPairingImportRow> rows) {
        Set<Integer> usedTableNumbers = new HashSet<>();

        for (MacMahonPairingImportRow row : rows) {
            if (!usedTableNumbers.add(row.tableNumber())) {
                throw new IllegalArgumentException(
                        "Die Tischnummer " + row.tableNumber() + " kommt in dieser Runde mehrfach vor."
                );
            }
        }
    }

    /**
     * Maps a validated MacMahon import row to the persistent pairing entity.
     *
     * @param tournament the owning tournament
     * @param imported the imported row
     * @return the pairing entity
     */
    private Pairing mapImportRowToPairing(Tournament tournament, MacMahonPairingImportRow imported) {
        validateImportRow(imported);

        return new Pairing(
                tournament,
                imported.roundNumber(),
                imported.tableNumber(),
                imported.blackPlayer().trim(),
                imported.whitePlayer().trim(),
                normalizeAndValidateSimpleResult(imported.result()),
                normalizeHandicap(imported.handicap()),
                imported.bye(),
                false
        );
    }

    /**
     * Performs structural validation on a single imported row before it is mapped.
     *
     * @param imported the imported row
     */
    private void validateImportRow(MacMahonPairingImportRow imported) {
        if (imported == null) {
            throw new IllegalArgumentException("Leerer MacMahon-Importdatensatz.");
        }

        if (imported.roundNumber() == null || imported.roundNumber() < 1) {
            throw new IllegalArgumentException("Ungültige Rundennummer im MacMahon-Import.");
        }

        if (imported.tableNumber() == null || imported.tableNumber() < 1) {
            throw new IllegalArgumentException("Ungültige Tischnummer im MacMahon-Import.");
        }

        if (imported.blackPlayer() == null || imported.blackPlayer().isBlank()) {
            throw new IllegalArgumentException(
                    "Für Tisch " + imported.tableNumber() + " fehlt der Name von Schwarz."
            );
        }

        if (imported.whitePlayer() == null || imported.whitePlayer().isBlank()) {
            throw new IllegalArgumentException(
                    "Für Tisch " + imported.tableNumber() + " fehlt der Name von Weiß."
            );
        }

        normalizeAndValidateSimpleResult(imported.result());
    }

    /**
     * Normalizes the handicap value used by the import.
     *
     * <p>The placeholder {@code "-"} is treated as no handicap and therefore
     * converted to {@code null}.</p>
     *
     * @param handicap the raw handicap value
     * @return the normalized handicap or {@code null}
     */
    private String normalizeHandicap(String handicap) {
        if (handicap == null || handicap.isBlank()) {
            return null;
        }

        String normalized = handicap.trim();

        if (normalized.equals("-")) {
            return null;
        }

        return normalized;
    }

    /**
     * Validates that all imported player names can be matched unambiguously to
     * registered participants of the tournament and to the selected round.
     *
     * <p>This validation is necessary because MacMahon imports identify players
     * by name instead of by internal database ID.</p>
     *
     * @param tournamentId the tournament ID
     * @param roundNumber the selected round
     * @param importedRows parsed import rows
     */
    private void validateImportedPlayersAgainstRegistrations(Long tournamentId,
                                                             Integer roundNumber,
                                                             List<MacMahonPairingImportRow> importedRows) {
        List<Registration> allRegistrations =
                registrationService.findStartListByTournamentId(tournamentId);

        List<Registration> registrationsForRound =
                registrationService.findStartListByTournamentIdAndRound(tournamentId, roundNumber);

        Map<String, Registration> allRegistrationsByName =
                buildUniqueRegistrationMap(allRegistrations, "dieses Turnier");

        Map<String, Registration> roundRegistrationsByName =
                buildUniqueRegistrationMap(registrationsForRound, "Runde " + roundNumber);

        Set<String> usedPlayersInRound = new HashSet<>();

        for (MacMahonPairingImportRow row : importedRows) {
            validateImportedPlayer(
                    row.blackPlayer(),
                    row.tableNumber(),
                    "Schwarz",
                    roundNumber,
                    allRegistrationsByName,
                    roundRegistrationsByName,
                    usedPlayersInRound
            );

            // Bye rows do not represent an actual opposing player in the round.
            if (!row.bye()) {
                validateImportedPlayer(
                        row.whitePlayer(),
                        row.tableNumber(),
                        "Weiß",
                        roundNumber,
                        allRegistrationsByName,
                        roundRegistrationsByName,
                        usedPlayersInRound
                );
            }
        }
    }

    /**
     * Validates one imported player reference against tournament registrations.
     *
     * @param importedName the player name from the import file
     * @param tableNumber the table number of the row
     * @param color the player color label used in validation messages
     * @param roundNumber the selected round
     * @param allRegistrationsByName all tournament registrations indexed by normalized name
     * @param roundRegistrationsByName round-specific registrations indexed by normalized name
     * @param usedPlayersInRound set used to detect duplicate player occurrences within the round
     */
    private void validateImportedPlayer(String importedName,
                                        Integer tableNumber,
                                        String color,
                                        Integer roundNumber,
                                        Map<String, Registration> allRegistrationsByName,
                                        Map<String, Registration> roundRegistrationsByName,
                                        Set<String> usedPlayersInRound) {

        String normalizedName = normalizeNameForLookup(importedName);

        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException(
                    "Für Tisch " + tableNumber + " fehlt der Name von " + color + "."
            );
        }

        if (!allRegistrationsByName.containsKey(normalizedName)) {
            throw new IllegalArgumentException(
                    "Die Person '" + importedName + "' an Tisch " + tableNumber +
                            " (" + color + ") ist für dieses Turnier nicht angemeldet."
            );
        }

        if (!roundRegistrationsByName.containsKey(normalizedName)) {
            throw new IllegalArgumentException(
                    "Die Person '" + importedName + "' an Tisch " + tableNumber +
                            " (" + color + ") ist nicht für Runde " + roundNumber + " spielberechtigt."
            );
        }

        if (!usedPlayersInRound.add(normalizedName)) {
            throw new IllegalArgumentException(
                    "Die Person '" + importedName + "' kommt in Runde " + roundNumber + " mehrfach vor."
            );
        }
    }

    /**
     * Builds a lookup map for registrations keyed by normalized participant name.
     *
     * <p>The method also ensures that no duplicate participant names exist in the
     * given registration context, because otherwise the MacMahon import would not
     * be able to match players unambiguously.</p>
     *
     * @param registrations registrations to index
     * @param contextLabel label used in validation messages
     * @return registration lookup map by normalized full name
     */
    private Map<String, Registration> buildUniqueRegistrationMap(List<Registration> registrations,
                                                                 String contextLabel) {
        Map<String, Registration> registrationsByName = new HashMap<>();

        for (Registration registration : registrations) {
            String fullName = registration.getParticipant().getFullName();
            String normalizedName = normalizeNameForLookup(fullName);

            Registration existing = registrationsByName.putIfAbsent(normalizedName, registration);

            if (existing != null) {
                throw new IllegalArgumentException(
                        "Der Name '" + fullName + "' kommt in den Anmeldungen für "
                                + contextLabel
                                + " mehrfach vor. Der MacMahon-Import ist damit nicht eindeutig möglich."
                );
            }
        }

        return registrationsByName;
    }

    /**
     * Normalizes a player name for case-insensitive matching across systems.
     *
     * <p>The method also converts the format {@code "LastName, FirstName"} into
     * {@code "FirstName LastName"} in order to support MacMahon exports that use
     * a comma-separated display format.</p>
     *
     * @param value the raw player name
     * @return normalized lookup key
     */
    private String normalizeNameForLookup(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value
                .trim()
                .replaceAll("\\s+", " ");

        if (normalized.contains(",")) {
            String[] parts = normalized.split(",", 2);
            String lastName = parts[0].trim();
            String firstName = parts[1].trim();
            normalized = (firstName + " " + lastName).trim();
        }

        return normalized.toLowerCase(Locale.ROOT);
    }
}