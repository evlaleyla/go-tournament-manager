package com.evlaleyla.gotournamentmanager.backend.pairing;

import com.evlaleyla.gotournamentmanager.backend.macmahon.MacMahonInterfaceService;
import com.evlaleyla.gotournamentmanager.backend.macmahon.MacMahonPairingImportRow;
import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PairingService {

    private final PairingRepository pairingRepository;
    private final TournamentRepository tournamentRepository;
    private final MacMahonInterfaceService macMahonInterfaceService;

    public PairingService(PairingRepository pairingRepository,
                          TournamentRepository tournamentRepository,
                          MacMahonInterfaceService macMahonInterfaceService) {
        this.pairingRepository = pairingRepository;
        this.tournamentRepository = tournamentRepository;
        this.macMahonInterfaceService = macMahonInterfaceService;
    }

    public List<Pairing> findByTournamentId(Long tournamentId) {
        return pairingRepository.findByTournamentIdOrderByRoundNumberAscTableNumberAsc(tournamentId);
    }

    public List<Pairing> findByTournamentIdAndRound(Long tournamentId, Integer roundNumber) {
        return pairingRepository.findByTournamentIdAndRoundNumberOrderByTableNumberAsc(tournamentId, roundNumber);
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
    public void importPairingsFromMacMahon(Long tournamentId, Integer roundNumber, MultipartFile file) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Turnier nicht gefunden: " + tournamentId));

        if (roundNumber == null || roundNumber < 1 || roundNumber > tournament.getNumberOfRounds()) {
            throw new IllegalArgumentException(
                    "Die Runde " + roundNumber + " liegt außerhalb der definierten Turnierrunden."
            );
        }

        List<MacMahonPairingImportRow> importedRows =
                macMahonInterfaceService.parsePairingsExport(file, roundNumber);

        if (importedRows.isEmpty()) {
            throw new IllegalArgumentException("Die Datei enthält keine Paarungen.");
        }

        validateUniqueTableNumbersFromRows(importedRows);

        for (MacMahonPairingImportRow imported : importedRows) {
            if (!roundNumber.equals(imported.roundNumber())) {
                throw new IllegalArgumentException(
                        "Die importierte Datei enthält Paarungen für eine andere Runde als die ausgewählte Runde " + roundNumber + "."
                );
            }
        }

        pairingRepository.deleteByTournamentIdAndRoundNumber(tournamentId, roundNumber);

        List<Pairing> entitiesToSave = importedRows.stream()
                .map(imported -> mapImportRowToPairing(tournament, imported))
                .toList();

        pairingRepository.saveAll(entitiesToSave);
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
                        "Die Tischnummer " + row.tableNumber() +
                                " kommt in dieser Runde mehrfach vor."
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
                imported.bye()
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
}