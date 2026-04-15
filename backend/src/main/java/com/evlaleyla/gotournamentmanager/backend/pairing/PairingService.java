package com.evlaleyla.gotournamentmanager.backend.pairing;

import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class PairingService {

    private final PairingRepository pairingRepository;
    private final TournamentRepository tournamentRepository;

    public PairingService(PairingRepository pairingRepository,
                          TournamentRepository tournamentRepository) {
        this.pairingRepository = pairingRepository;
        this.tournamentRepository = tournamentRepository;
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
        pairing.setResult(normalizeAndValidateResultCode(result));
        return pairingRepository.save(pairing);
    }

    @Transactional
    public void replacePairingsFromCsv(Long tournamentId, Integer expectedRound, MultipartFile file) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Turnier nicht gefunden: " + tournamentId));

        List<Pairing> pairings = parseCsv(file, tournament);

        if (pairings.isEmpty()) {
            throw new IllegalArgumentException("Die CSV-Datei enthält keine Paarungen.");
        }

        Integer roundNumber = pairings.get(0).getRoundNumber();

        boolean containsDifferentRounds = pairings.stream()
                .anyMatch(pairing -> !pairing.getRoundNumber().equals(roundNumber));

        if (containsDifferentRounds) {
            throw new IllegalArgumentException("Bitte pro Upload nur Paarungen für genau eine Runde verwenden.");
        }

        if (roundNumber < 1 || roundNumber > tournament.getNumberOfRounds()) {
            throw new IllegalArgumentException(
                    "Die Runde " + roundNumber + " liegt außerhalb der definierten Turnierrunden."
            );
        }

        if (expectedRound != null && !roundNumber.equals(expectedRound)) {
            throw new IllegalArgumentException(
                    "Die hochgeladene CSV enthält Paarungen für Runde " + roundNumber +
                            ", wurde aber im Bereich für Runde " + expectedRound + " hochgeladen."
            );
        }

        validateUniqueTableNumbers(pairings);


        pairingRepository.deleteByTournamentIdAndRoundNumber(tournamentId, roundNumber);
        pairingRepository.saveAll(pairings);
    }

    private List<Pairing> parseCsv(MultipartFile file, Tournament tournament) {
        List<Pairing> pairings = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                List<String> columns = parseCsvLine(line);

                if (columns.size() < 4) {
                    throw new IllegalArgumentException(
                            "Ungültiges CSV-Format in Zeile " + lineNumber +
                                    ". Erwartet werden mindestens 4 Spalten."
                    );
                }

                Integer roundNumber = parseInteger(columns.get(0), "Runde", lineNumber);
                Integer tableNumber = parseInteger(columns.get(1), "Tisch", lineNumber);
                String blackPlayer = columns.size() > 2 ? columns.get(2).trim() : "";
                String whitePlayer = columns.size() > 3 ? columns.get(3).trim() : "";
                String result = columns.size() > 4 ? columns.get(4).trim() : "";

                if (blackPlayer.isBlank()) {
                    throw new IllegalArgumentException("Schwarz-Spieler fehlt in Zeile " + lineNumber + ".");
                }

                if (whitePlayer.isBlank()) {
                    throw new IllegalArgumentException("Weiß-Spieler fehlt in Zeile " + lineNumber + ".");
                }

                pairings.add(new Pairing(
                        tournament,
                        roundNumber,
                        tableNumber,
                        blackPlayer,
                        whitePlayer,
                        normalizeAndValidateResultCode(result)
                ));
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Die CSV-Datei konnte nicht gelesen werden.", e);
        }

        return pairings;
    }

    private Integer parseInteger(String value, String fieldName, int lineNumber) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    fieldName + " ist in Zeile " + lineNumber + " keine gültige Zahl."
            );
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ';' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        result.add(current.toString());
        return result;
    }

    private String normalizeAndValidateResultCode(String result) {
        if (result == null || result.isBlank()) {
            return null;
        }

        String normalized = result.trim().toUpperCase().replace(",", ".");

        if ("0".equals(normalized)) {
            return normalized;
        }

        if (normalized.matches("^[BW]\\+[RTF]$")) {
            return normalized;
        }

        if (normalized.matches("^[BW]\\+\\d+(\\.\\d+)?$")) {
            return normalized;
        }

        throw new IllegalArgumentException(
                "Ungültiger Ergebniscode: '" + result + "'. Erlaubt sind z. B. B+R, W+T, B+F, W+5.5 oder 0."
        );
    }

    private void validateUniqueTableNumbers(List<Pairing> pairings) {
        java.util.Set<Integer> usedTableNumbers = new java.util.HashSet<>();

        for (Pairing pairing : pairings) {
            if (!usedTableNumbers.add(pairing.getTableNumber())) {
                throw new IllegalArgumentException(
                        "Die Tischnummer " + pairing.getTableNumber() +
                                " kommt in dieser Runde mehrfach vor. Jede Tischnummer darf pro Runde nur einmal vorkommen."
                );
            }
        }
    }
}