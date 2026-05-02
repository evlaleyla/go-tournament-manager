package com.evlaleyla.gotournamentmanager.backend.macmahon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for parsing MacMahon exports used by the application.
 *
 * <p>This service supports two main import types:
 * <ul>
 *     <li>pairing exports for a specific round</li>
 *     <li>wall-list exports containing standings data</li>
 * </ul>
 *
 * <p>The parser accepts both delimited and fixed-format pairing files where possible.
 */
@Service
public class MacMahonInterfaceService {

    private static final Logger log = LoggerFactory.getLogger(MacMahonInterfaceService.class);

    private static final Pattern NON_WHITESPACE_PATTERN = Pattern.compile("\\S+");

    /**
     * Pattern for fixed-width / line-based pairing exports.
     *
     * <p>Supported result formats include open results, normal wins, jigo,
     * and special cases such as both lose / both win.
     */
    private static final Pattern PAIRING_LINE_PATTERN = Pattern.compile(
            "^\\s*(\\d+)\\s+(.+?)\\s+-\\s+(.+?)\\s+(\\?-\\?|1-0|0-1|0-0|1-1|=|jigo|Jigo|-|\\?)(?:\\s+(.*))?\\s*$"
    );

    private static final Pattern WALL_LIST_ROW_PATTERN = Pattern.compile(
            "^\\s*(\\(?\\d+\\)?)\\s+(.+?)\\s{2,}(?:(\\S+)\\s+)?(\\d+\\s+(?:Kyu|Dan|Pro))\\s{2,}(\\S+)\\s+(.*)$"
    );

    private static final String HEADER_PLACE = "place";
    private static final String HEADER_NAME = "name";
    private static final String HEADER_CLUB = "club";
    private static final String HEADER_LEVEL = "level";
    private static final String HEADER_SCORE = "score";
    private static final String HEADER_POINTS = "points";
    private static final String HEADER_SCORE_X = "scorex";
    private static final String HEADER_SOS = "sos";
    private static final String HEADER_SOSOS = "sosos";

    /**
     * Parses a MacMahon pairing export for the given round.
     *
     * <p>The method first tries to detect a delimited export with a recognizable
     * header. If that is not possible, it falls back to a fixed-format parser.
     *
     * @param file the uploaded MacMahon export file
     * @param roundNumber the round number chosen by the user
     * @return parsed pairing rows
     */
    public List<MacMahonPairingImportRow> parsePairingsExport(MultipartFile file, Integer roundNumber) {
        log.info("Starting MacMahon pairing import parsing for roundNumber={}", roundNumber);

        List<String> lines = readLines(file);
        String headerLine = findPairingHeader(lines);

        if (headerLine != null) {
            String delimiterRegex = detectDelimiter(headerLine);

            if (delimiterRegex != null) {
                log.debug("Detected delimited MacMahon pairing export for roundNumber={}", roundNumber);
                return parseDelimitedPairings(lines, headerLine, delimiterRegex, roundNumber);
            }
        }

        log.debug("Falling back to fixed-format MacMahon pairing parser for roundNumber={}", roundNumber);
        return parseFixedFormatPairings(lines, roundNumber);
    }

    /**
     * Parses a MacMahon wall-list export.
     *
     * <p>The parser first validates the required header columns and derives the
     * number of round columns from the header. Each data row is then parsed with
     * a dedicated pattern that supports empty clubs and multi-character round
     * status values such as "free".</p>
     *
     * @param file the uploaded wall-list file
     * @return parsed wall-list entries
     */
    public List<MacMahonWallListEntry> parseWallList(MultipartFile file) {
        log.info("Starting MacMahon wall-list parsing");

        List<String> lines = readLines(file);
        List<MacMahonWallListEntry> entries = new ArrayList<>();

        String headerLine = findWallListHeader(lines);
        if (headerLine == null) {
            log.warn("Wall-list parsing failed because no header line was found");
            throw new IllegalArgumentException("Kein Wall-List-Header gefunden.");
        }

        List<String> headerTokens = extractHeaderTokens(headerLine);
        List<String> normalizedHeaders = headerTokens.stream()
                .map(token -> token.toLowerCase(Locale.ROOT))
                .toList();

        int placeIndex = normalizedHeaders.indexOf(HEADER_PLACE);
        int nameIndex = normalizedHeaders.indexOf(HEADER_NAME);
        int clubIndex = normalizedHeaders.indexOf(HEADER_CLUB);
        int levelIndex = normalizedHeaders.indexOf(HEADER_LEVEL);
        int scoreIndex = normalizedHeaders.indexOf(HEADER_SCORE);
        int pointsIndex = normalizedHeaders.indexOf(HEADER_POINTS);
        int scoreXIndex = normalizedHeaders.indexOf(HEADER_SCORE_X);
        int sosIndex = normalizedHeaders.indexOf(HEADER_SOS);
        int sososIndex = normalizedHeaders.indexOf(HEADER_SOSOS);

        validateWallListColumnIndexes(
                placeIndex,
                nameIndex,
                clubIndex,
                levelIndex,
                scoreIndex,
                pointsIndex,
                scoreXIndex,
                sosIndex,
                sososIndex
        );

        int roundColumnStartIndex = scoreIndex + 1;
        int roundColumnEndExclusive = pointsIndex;
        int roundColumnCount = roundColumnEndExclusive - roundColumnStartIndex;

        if (roundColumnCount < 0) {
            log.warn("Wall-list parsing failed because round columns could not be determined");
            throw new IllegalArgumentException("Die Rundenspalten der Wall List konnten nicht erkannt werden.");
        }

        for (String line : lines) {
            if (shouldSkipWallListDataLine(line, headerLine)) {
                continue;
            }

            MacMahonWallListEntry entry = parseWallListRow(line, roundColumnCount);

            if (entry != null) {
                entries.add(entry);
            }
        }

        if (entries.isEmpty()) {
            log.warn("Wall-list parsing failed because no readable entries were found");
            throw new IllegalArgumentException(
                    "Die Datei enthält keine lesbaren Wall-List-Daten im erwarteten MacMahon-Format."
            );
        }

        log.info("Successfully parsed {} wall-list entries", entries.size());
        return entries;
    }

    private MacMahonWallListEntry parseWallListRow(String line, int roundColumnCount) {
        Matcher matcher = WALL_LIST_ROW_PATTERN.matcher(line);

        if (!matcher.matches()) {
            log.warn("Wall-list row could not be parsed: {}", line);
            throw new IllegalArgumentException(
                    "Die Wall-List-Zeile konnte nicht gelesen werden: " + line
            );
        }

        Integer place = tryParsePlace(matcher.group(1));
        if (place == null) {
            throw new IllegalArgumentException(
                    "Die Platz-Spalte der Wall-List konnte nicht gelesen werden: " + line
            );
        }

        String name = matcher.group(2).trim();
        String club = matcher.group(3) == null ? "" : matcher.group(3).trim();
        String level = matcher.group(4).trim();
        String score = matcher.group(5).trim();

        List<String> tailTokens = java.util.Arrays.stream(matcher.group(6).trim().split("\\s+"))
                .toList();

        if (tailTokens.size() != roundColumnCount + 4) {
            throw new IllegalArgumentException(
                    "Die Wall-List-Zeile konnte nicht eindeutig gelesen werden: " + line
            );
        }

        List<String> roundStatuses = new ArrayList<>();
        for (int i = 0; i < roundColumnCount; i++) {
            roundStatuses.add(tailTokens.get(i));
        }

        Integer points = tryParseInteger(tailTokens.get(roundColumnCount));
        if (points == null) {
            throw new IllegalArgumentException(
                    "Die Punkte-Spalte der Wall-List konnte nicht gelesen werden: " + line
            );
        }

        String scoreX = tailTokens.get(roundColumnCount + 1);
        String sos = tailTokens.get(roundColumnCount + 2);
        String sosos = tailTokens.get(roundColumnCount + 3);

        return new MacMahonWallListEntry(
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
        );
    }

    /**
     * Finds the header line of a wall-list export.
     */
    private String findWallListHeader(List<String> lines) {
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            if (line.trim().startsWith("Place")) {
                return line;
            }
        }

        return null;
    }

    /**
     * Verifies that all required wall-list columns are present.
     */
    private void validateWallListColumnIndexes(int placeIndex,
                                               int nameIndex,
                                               int clubIndex,
                                               int levelIndex,
                                               int scoreIndex,
                                               int pointsIndex,
                                               int scoreXIndex,
                                               int sosIndex,
                                               int sososIndex) {
        if (placeIndex < 0
                || nameIndex < 0
                || clubIndex < 0
                || levelIndex < 0
                || scoreIndex < 0
                || pointsIndex < 0
                || scoreXIndex < 0
                || sosIndex < 0
                || sososIndex < 0) {
            log.warn("Wall-list parsing failed because one or more required columns were missing");
            throw new IllegalArgumentException("Die Wall-List-Spalten konnten nicht vollständig erkannt werden.");
        }
    }

    /**
     * Determines whether a wall-list data line should be skipped.
     */
    private boolean shouldSkipWallListDataLine(String line, String headerLine) {
        if (line == null || line.isBlank()) {
            return true;
        }

        String trimmed = line.trim();

        if (trimmed.startsWith("[")) {
            return true;
        }

        return line.equals(headerLine) || trimmed.startsWith("Place");
    }

    /**
     * Extracts visible header tokens from the wall-list header line.
     */
    private List<String> extractHeaderTokens(String headerLine) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = NON_WHITESPACE_PATTERN.matcher(headerLine);

        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        return tokens;
    }


    /**
     * Converts a MacMahon result token into the internal result representation.
     *
     * <p>Internal mapping:
     * <ul>
     *     <li>B = black wins</li>
     *     <li>W = white wins</li>
     *     <li>J = jigo</li>
     *     <li>L = both lose</li>
     *     <li>D = both win</li>
     *     <li>null = result still open</li>
     * </ul>
     */
    private String normalizeMacMahonResult(String macMahonResult) {
        if (macMahonResult == null || macMahonResult.isBlank()) {
            return null;
        }

        String normalized = macMahonResult.trim();

        // MacMahon may mark referee decisions with "(!)".
        normalized = normalized.replace("(!)", "").trim();

        return switch (normalized) {
            case "?-?", "-", "?" -> null;
            case "1-0" -> "B";
            case "0-1" -> "W";
            case "0-0" -> "L";
            case "1-1" -> "D";
            case "=", "jigo", "Jigo" -> "J";
            default -> {
                log.warn("Unknown MacMahon result token encountered: {}", macMahonResult);
                throw new IllegalArgumentException("Unbekanntes MacMahon-Ergebnisformat: " + macMahonResult);
            }
        };
    }

    /**
     * Reads all lines from the uploaded file using UTF-8.
     */
    private List<String> readLines(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            List<String> lines = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            log.debug("Read {} lines from MacMahon input file", lines.size());
            return lines;

        } catch (IOException e) {
            log.warn("Failed to read MacMahon input file", e);
            throw new IllegalArgumentException("Die MacMahon-Datei konnte nicht gelesen werden.", e);
        }
    }

    /**
     * Searches for a pairing header in English or German.
     */
    private String findPairingHeader(List<String> lines) {
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String normalized = line.trim().toLowerCase(Locale.ROOT);

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

    /**
     * Detects the delimiter used by a header line.
     *
     * @return a regex suitable for String#split, or null if no delimiter was detected
     */
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

    /**
     * Parses a delimiter-based pairing export.
     */
    private List<MacMahonPairingImportRow> parseDelimitedPairings(List<String> lines,
                                                                  String headerLine,
                                                                  String delimiterRegex,
                                                                  Integer roundNumber) {
        List<String> headers = splitDelimited(headerLine, delimiterRegex).stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();

        int boardIndex = findHeaderIndex(headers, "board", "brett");
        int blackIndex = findHeaderIndex(headers, "black", "schwarz");
        int whiteIndex = findHeaderIndex(headers, "white", "weiß", "weiss");
        int resultIndex = findHeaderIndex(headers, "result", "ergebnis");
        int handicapIndex = findHeaderIndex(headers, "handicap", "vorgabe");

        if (boardIndex < 0 || blackIndex < 0 || whiteIndex < 0 || resultIndex < 0) {
            log.warn("Delimited pairing parsing failed because required columns were missing");
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

            Integer tableNumber = tryParseInteger(columns.get(boardIndex));
            if (tableNumber == null) {
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

            validatePlayerName(blackPlayer, tableNumber, "Schwarz");
            validatePlayerName(whitePlayer, tableNumber, "Weiß");

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
            log.warn("Delimited pairing parsing failed because no readable pairings were found");
            throw new IllegalArgumentException("Die Pairing-Datei enthält keine lesbaren Paarungen.");
        }

        log.info("Successfully parsed {} delimited pairing rows for roundNumber={}", rows.size(), roundNumber);
        return rows;
    }

    /**
     * Parses a fixed-format pairing export line by line.
     */
    private List<MacMahonPairingImportRow> parseFixedFormatPairings(List<String> lines, Integer roundNumber) {
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
                log.warn("Skipping unreadable pairing line: {}", line);
                continue;
            }

            Integer tableNumber = Integer.parseInt(matcher.group(1));
            String blackName = cleanMacMahonPlayerDisplay(matcher.group(2));
            String whiteName = cleanMacMahonPlayerDisplay(matcher.group(3));
            String result = normalizeMacMahonResult(matcher.group(4));
            String handicap = matcher.group(5);

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
            log.warn("Fixed-format pairing parsing failed because no readable pairings were found");
            throw new IllegalArgumentException(
                    "Die Datei enthält keine lesbaren Paarungen im unterstützten MacMahon-Format."
            );
        }

        log.info("Successfully parsed {} fixed-format pairing rows for roundNumber={}", rows.size(), roundNumber);
        return rows;
    }

    /**
     * Splits a delimiter-based line while preserving empty columns.
     */
    private List<String> splitDelimited(String line, String delimiterRegex) {
        return java.util.Arrays.stream(line.split(delimiterRegex, -1))
                .map(String::trim)
                .toList();
    }

    /**
     * Finds a column index by exact match first and partial match second.
     */
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

    /**
     * Removes trailing MacMahon metadata from a player display name.
     *
     * <p>Example:
     * {@code "Müller, Anna (3d)"} becomes {@code "Müller, Anna"}.
     */
    private String cleanMacMahonPlayerDisplay(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();

        int metadataStart = cleaned.indexOf(" (");
        if (metadataStart > 0 && cleaned.endsWith(")")) {
            cleaned = cleaned.substring(0, metadataStart).trim();
        }

        return cleaned;
    }

    /**
     * Determines whether a player token represents a bye entry.
     */
    private boolean isBye(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        return normalized.equals("bye")
                || normalized.equals("freilos")
                || normalized.equals("spielfrei");
    }

    /**
     * Validates that a player name is present in a pairing row.
     */
    private void validatePlayerName(String playerName, Integer tableNumber, String colorLabel) {
        if (playerName.isBlank()) {
            log.warn("Pairing row is missing a player name. tableNumber={}, color={}", tableNumber, colorLabel);
            throw new IllegalArgumentException(
                    "In Zeile mit Tisch " + tableNumber + " fehlt der Name von " + colorLabel + "."
            );
        }
    }

    /**
     * Parses the place token of a wall-list row.
     *
     * <p>Some files contain place values such as "(1)", which are normalized before parsing.</p>
     */
    private Integer tryParsePlace(String rawPlaceToken) {
        String placeToken = rawPlaceToken
                .replace("(", "")
                .replace(")", "")
                .trim();

        return tryParseInteger(placeToken);
    }

    /**
     * Tries to parse an integer and returns null on failure.
     */
    private Integer tryParseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }
}