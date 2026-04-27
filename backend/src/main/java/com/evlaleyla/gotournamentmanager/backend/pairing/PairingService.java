package com.evlaleyla.gotournamentmanager.backend.pairing;

import com.evlaleyla.gotournamentmanager.backend.macmahon.MacMahonInterfaceService;
import com.evlaleyla.gotournamentmanager.backend.macmahon.MacMahonPairingImportRow;
import com.evlaleyla.gotournamentmanager.backend.registration.Registration;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationService;
import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentRepository;
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

@Service
public class PairingService {

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

    public List<Pairing> findByTournamentId(Long tournamentId) {
        return pairingRepository.findByTournamentIdOrderByRoundNumberAscTableNumberAsc(tournamentId);
    }

    public List<Pairing> findByTournamentIdAndRound(Long tournamentId, Integer roundNumber) {
        return pairingRepository.findByTournamentIdAndRoundNumberOrderByTableNumberAsc(tournamentId, roundNumber);
    }

    public List<Pairing> findPublishedByTournamentIdAndRound(Long tournamentId, Integer roundNumber) {
        return pairingRepository.findByTournamentIdAndRoundNumberAndPublishedTrueOrderByTableNumberAsc(
                tournamentId,
                roundNumber
        );
    }

    public Set<Integer> findPublishedRoundNumbers(Long tournamentId) {
        return pairingRepository.findByTournamentIdOrderByRoundNumberAscTableNumberAsc(tournamentId)
                .stream()
                .filter(Pairing::isPublished)
                .map(Pairing::getRoundNumber)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public Pairing findById(Long id) {
        return pairingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paarung nicht gefunden: " + id));
    }

    public Pairing updateResult(Long pairingId, String result) {
        Pairing pairing = findById(pairingId);
        pairing.setResult(normalizeAndValidateSimpleResult(result));
        return pairingRepository.save(pairing);
    }

    @Transactional
    public void publishRound(Long tournamentId, Integer roundNumber) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Turnier nicht gefunden: " + tournamentId));

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
    }

    @Transactional
    public void unpublishRound(Long tournamentId, Integer roundNumber) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Turnier nicht gefunden: " + tournamentId));

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
    }

    @Transactional
    public void importPairingsFromMacMahon(Long tournamentId, Integer roundNumber, MultipartFile file) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Turnier nicht gefunden: " + tournamentId));

        validateRoundNumber(tournament, roundNumber);

        List<MacMahonPairingImportRow> importedRows =
                macMahonInterfaceService.parsePairingsExport(file, roundNumber);

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

        pairingRepository.deleteByTournamentIdAndRoundNumber(tournamentId, roundNumber);
        pairingRepository.flush();

        List<Pairing> entitiesToSave = importedRows.stream()
                .map(imported -> mapImportRowToPairing(tournament, imported))
                .toList();

        pairingRepository.saveAll(entitiesToSave);
        pairingRepository.flush();
    }

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

    private String normalizeAndValidateSimpleResult(String result) {
        if (result == null || result.isBlank()) {
            return null;
        }

        String normalized = result.trim().toUpperCase();

        if (!normalized.equals("B")
                && !normalized.equals("W")
                && !normalized.equals("J")
                && !normalized.equals("L")
                && !normalized.equals("D")) {
            throw new IllegalArgumentException("Ungültiges Ergebnis. Erlaubt sind B, W, J, L, D oder leer.");
        }

        return normalized;
    }

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

    private Map<String, Registration> buildUniqueRegistrationMap(List<Registration> registrations,
                                                                 String contextLabel) {
        Map<String, Registration> registrationsByName = new HashMap<>();

        for (Registration registration : registrations) {
            String fullName = registration.getParticipant().getFullName();
            String normalizedName = normalizeNameForLookup(fullName);

            Registration existing = registrationsByName.putIfAbsent(normalizedName, registration);

            if (existing != null) {
                throw new IllegalArgumentException(
                        "Der Name '" + fullName + "' kommt in den Anmeldungen für " +
                                contextLabel +
                                " mehrfach vor. Der MacMahon-Import ist damit nicht eindeutig möglich."
                );
            }
        }

        return registrationsByName;
    }

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