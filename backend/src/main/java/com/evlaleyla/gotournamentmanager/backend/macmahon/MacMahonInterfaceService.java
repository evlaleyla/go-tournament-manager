package com.evlaleyla.gotournamentmanager.backend.macmahon;


import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MacMahonInterfaceService {

    private static final Pattern PAIRING_LINE_PATTERN =
            Pattern.compile("^\\s*(\\d+)\\s+(.+?)(?:\\s+\\(([^)]*)\\))?\\s+-\\s+(.+?)(?:\\s+\\(([^)]*)\\))?\\s+(\\?-\\?|1-0|0-1|0-0|1-1|=|jigo|Jigo|-|\\?)(?:\\s+(\\S+))?\\s*$");


    public List<MacMahonPairingImportRow> parsePairingsExport(MultipartFile file, Integer roundNumber) {
        List<String> lines = readLines(file);

        String headerLine = findPairingHeader(lines);

        if (headerLine != null) {
            String delimiterRegex = detectDelimiter(headerLine);

            if (delimiterRegex != null) {
                return parseDelimitedPairings(lines, headerLine, delimiterRegex, roundNumber);
            }
        }

        return parseFixedFormatPairings(lines, roundNumber);
    }

    public List<MacMahonWallListEntry> parseWallList(MultipartFile file) {
        List<String> lines = readLines(file);
        List<MacMahonWallListEntry> entries = new ArrayList<>();

        String headerLine = null;

        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            if (line.trim().startsWith("Place")) {
                headerLine = line;
                break;
            }
        }

        if (headerLine == null) {
            throw new IllegalArgumentException("Kein Wall-List-Header gefunden.");
        }

        List<String> headerTokens = extractHeaderTokens(headerLine);
        List<Integer> columnStarts = extractColumnStarts(headerLine);

        List<String> normalizedHeaders = headerTokens.stream()
                .map(token -> token.toLowerCase(java.util.Locale.ROOT))
                .toList();

        int placeIndex = normalizedHeaders.indexOf("place");
        int nameIndex = normalizedHeaders.indexOf("name");
        int clubIndex = normalizedHeaders.indexOf("club");
        int levelIndex = normalizedHeaders.indexOf("level");
        int scoreIndex = normalizedHeaders.indexOf("score");
        int pointsIndex = normalizedHeaders.indexOf("points");
        int scoreXIndex = normalizedHeaders.indexOf("scorex");
        int sosIndex = normalizedHeaders.indexOf("sos");
        int sososIndex = normalizedHeaders.indexOf("sosos");

        if (placeIndex < 0
                || nameIndex < 0
                || clubIndex < 0
                || levelIndex < 0
                || scoreIndex < 0
                || pointsIndex < 0
                || scoreXIndex < 0
                || sosIndex < 0
                || sososIndex < 0) {
            throw new IllegalArgumentException("Die Wall-List-Spalten konnten nicht vollständig erkannt werden.");
        }

        int roundColumnStartIndex = scoreIndex + 1;
        int roundColumnEndExclusive = pointsIndex;
        int roundColumnCount = roundColumnEndExclusive - roundColumnStartIndex;

        if (roundColumnCount < 0) {
            throw new IllegalArgumentException("Die Rundenspalten der Wall List konnten nicht erkannt werden.");
        }

        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String trimmed = line.trim();

            if (trimmed.startsWith("[")) {
                continue;
            }

            if (line.equals(headerLine) || trimmed.startsWith("Place")) {
                continue;
            }

            List<String> columns = splitFixedWidthColumns(line, columnStarts);

            if (columns.size() < headerTokens.size()) {
                continue;
            }

            String placeToken = columns.get(placeIndex)
                    .replace("(", "")
                    .replace(")", "")
                    .trim();

            Integer place;
            try {
                place = Integer.parseInt(placeToken);
            } catch (NumberFormatException e) {
                continue;
            }

            String name = columns.get(nameIndex).trim();
            String club = columns.get(clubIndex).trim();
            String level = columns.get(levelIndex).trim();
            String score = columns.get(scoreIndex).trim();
            String pointsToken = columns.get(pointsIndex).trim();
            String scoreX = columns.get(scoreXIndex).trim();
            String sos = columns.get(sosIndex).trim();
            String sosos = columns.get(sososIndex).trim();

            Integer points;
            try {
                points = Integer.parseInt(pointsToken);
            } catch (NumberFormatException e) {
                continue;
            }

            List<String> roundStatuses = new ArrayList<>();
            for (int i = roundColumnStartIndex; i < roundColumnEndExclusive; i++) {
                roundStatuses.add(columns.get(i).trim());
            }

            entries.add(new MacMahonWallListEntry(
                    place,
                    name,
                    club,
                    level,
                    score,
                    roundStatuses,
                    points,
                    scoreX,
                    sos,
                    sosos
            ));
        }

        if (entries.isEmpty()) {
            throw new IllegalArgumentException(
                    "Die Datei enthält keine lesbaren Wall-List-Daten im erwarteten MacMahon-Format."
            );
        }

        return entries;
    }

    private List<String> extractHeaderTokens(String headerLine) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\S+").matcher(headerLine);

        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        return tokens;
    }

    private List<Integer> extractColumnStarts(String headerLine) {
        List<Integer> starts = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\S+").matcher(headerLine);

        while (matcher.find()) {
            starts.add(matcher.start());
        }

        return starts;
    }

    private List<String> splitFixedWidthColumns(String line, List<Integer> columnStarts) {
        List<String> columns = new ArrayList<>();

        for (int i = 0; i < columnStarts.size(); i++) {
            int start = Math.min(columnStarts.get(i), line.length());
            int end = (i + 1 < columnStarts.size())
                    ? Math.min(columnStarts.get(i + 1), line.length())
                    : line.length();

            if (start >= end) {
                columns.add("");
            } else {
                columns.add(line.substring(start, end).trim());
            }
        }

        return columns;
    }

    private List<String> splitByWhitespace(String line) {
        return java.util.Arrays.stream(line.trim().split("\\s+"))
                .filter(token -> !token.isBlank())
                .toList();
    }

    private String normalizeMacMahonResult(String macMahonResult) {
        if (macMahonResult == null || macMahonResult.isBlank()) {
            return null;
        }

        String normalized = macMahonResult.trim();

        // MacMahon kann Schiedsrichterentscheidungen mit (!) markieren.
        normalized = normalized.replace("(!)", "").trim();

        return switch (normalized) {
            case "?-?", "-", "?" -> null;
            case "1-0" -> "B";
            case "0-1" -> "W";
            case "0-0" -> "L";
            case "1-1" -> "D";
            case "=", "jigo", "Jigo" -> "J";
            default -> throw new IllegalArgumentException(
                    "Unbekanntes MacMahon-Ergebnisformat: " + macMahonResult
            );
        };
    }

    private List<String> readLines(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            List<String> lines = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            return lines;

        } catch (IOException e) {
            throw new IllegalArgumentException("Die MacMahon-Datei konnte nicht gelesen werden.", e);
        }
    }

    private String findPairingHeader(List<String> lines) {
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String normalized = line.trim().toLowerCase(java.util.Locale.ROOT);

            boolean englishHeader = normalized.contains("board")
                    && normalized.contains("black")
                    && normalized.contains("white")
                    && normalized.contains("result");

            boolean germanHeader = normalized.contains("brett")
                    && normalized.contains("schwarz")
                    && (normalized.contains("weiß") || normalized.contains("weiss"))
                    && normalized.contains("ergebnis");

            if (englishHeader || germanHeader) {
                return line;
            }
        }

        return null;
    }

    private String detectDelimiter(String headerLine) {
        if (headerLine.contains("|")) {
            return "\\|";
        }

        if (headerLine.contains(";")) {
            return ";";
        }

        if (headerLine.contains("\t")) {
            return "\t";
        }

        return null;
    }

    private List<MacMahonPairingImportRow> parseDelimitedPairings(
            List<String> lines,
            String headerLine,
            String delimiterRegex,
            Integer roundNumber
    ) {
        List<String> headers = splitDelimited(headerLine, delimiterRegex)
                .stream()
                .map(value -> value.toLowerCase(java.util.Locale.ROOT))
                .toList();

        int boardIndex = findHeaderIndex(headers, "board", "brett");
        int blackIndex = findHeaderIndex(headers, "black", "schwarz");
        int whiteIndex = findHeaderIndex(headers, "white", "weiß", "weiss");
        int resultIndex = findHeaderIndex(headers, "result", "ergebnis");
        int handicapIndex = findHeaderIndex(headers, "handicap", "vorgabe");

        if (boardIndex < 0 || blackIndex < 0 || whiteIndex < 0 || resultIndex < 0) {
            throw new IllegalArgumentException(
                    "Die Pairing-Datei enthält nicht alle erforderlichen Spalten: Board/Brett, Black/Schwarz, White/Weiß und Result/Ergebnis."
            );
        }

        boolean dataSectionStarted = false;
        List<MacMahonPairingImportRow> rows = new ArrayList<>();

        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            if (!dataSectionStarted) {
                if (line.equals(headerLine)) {
                    dataSectionStarted = true;
                }
                continue;
            }

            List<String> columns = splitDelimited(line, delimiterRegex);

            int maxRequiredIndex = Math.max(
                    Math.max(boardIndex, blackIndex),
                    Math.max(whiteIndex, resultIndex)
            );

            if (columns.size() <= maxRequiredIndex) {
                continue;
            }

            Integer tableNumber;

            try {
                tableNumber = Integer.parseInt(columns.get(boardIndex).trim());
            } catch (NumberFormatException e) {
                continue;
            }

            String blackPlayer = cleanMacMahonPlayerDisplay(columns.get(blackIndex));
            String whitePlayer = cleanMacMahonPlayerDisplay(columns.get(whiteIndex));
            String result = normalizeMacMahonResult(columns.get(resultIndex));

            String handicap = null;
            if (handicapIndex >= 0 && handicapIndex < columns.size()) {
                handicap = columns.get(handicapIndex).trim();
            }

            boolean bye = isBye(whitePlayer);

            if (blackPlayer.isBlank()) {
                throw new IllegalArgumentException(
                        "In Zeile mit Tisch " + tableNumber + " fehlt der Name von Schwarz."
                );
            }

            if (whitePlayer.isBlank()) {
                throw new IllegalArgumentException(
                        "In Zeile mit Tisch " + tableNumber + " fehlt der Name von Weiß."
                );
            }

            rows.add(new MacMahonPairingImportRow(
                    roundNumber,
                    tableNumber,
                    blackPlayer,
                    whitePlayer,
                    result,
                    handicap,
                    bye
            ));
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "Die Pairing-Datei enthält keine lesbaren Paarungen."
            );
        }

        return rows;
    }

    private List<MacMahonPairingImportRow> parseFixedFormatPairings(
            List<String> lines,
            Integer roundNumber
    ) {
        List<MacMahonPairingImportRow> rows = new ArrayList<>();

        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            if (line.startsWith("[") || line.trim().startsWith("Board")) {
                continue;
            }

            Matcher matcher = PAIRING_LINE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                continue;
            }

            Integer tableNumber = Integer.parseInt(matcher.group(1));
            String blackName = cleanMacMahonPlayerDisplay(matcher.group(2));
            String whiteName = cleanMacMahonPlayerDisplay(matcher.group(4));
            String result = normalizeMacMahonResult(matcher.group(6));
            String handicap = matcher.group(7);

            rows.add(new MacMahonPairingImportRow(
                    roundNumber,
                    tableNumber,
                    blackName,
                    whiteName,
                    result,
                    handicap,
                    isBye(whiteName)
            ));
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "Die Datei enthält keine lesbaren Paarungen im unterstützten MacMahon-Format."
            );
        }

        return rows;
    }

    private List<String> splitDelimited(String line, String delimiterRegex) {
        return java.util.Arrays.stream(line.split(delimiterRegex, -1))
                .map(String::trim)
                .toList();
    }

    private int findHeaderIndex(List<String> headers, String... candidates) {
        for (String candidate : candidates) {
            int exactIndex = headers.indexOf(candidate);
            if (exactIndex >= 0) {
                return exactIndex;
            }
        }

        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);

            for (String candidate : candidates) {
                if (header.contains(candidate)) {
                    return i;
                }
            }
        }

        return -1;
    }

    private String cleanMacMahonPlayerDisplay(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();

        // Entfernt Rang-/Score-Angaben am Ende, z. B. "Müller, Anna (3d)".
        cleaned = cleaned.replaceAll("\\s*\\([^)]*\\)\\s*$", "").trim();

        return cleaned;
    }

    private boolean isBye(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);

        return normalized.equals("bye")
                || normalized.equals("freilos")
                || normalized.equals("spielfrei");
    }
}