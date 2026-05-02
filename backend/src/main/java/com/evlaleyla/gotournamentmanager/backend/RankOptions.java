package com.evlaleyla.gotournamentmanager.backend;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class for working with Go rank values.
 *
 * <p>This class provides:
 * <ul>
 *     <li>a predefined list of supported ranks,</li>
 *     <li>normalization of user-provided rank values,</li>
 *     <li>validation helpers for rank strings.</li>
 * </ul>
 *
 * <p>Supported rank types:
 * <ul>
 *     <li>Kyu: 30k to 1k</li>
 *     <li>Dan: 1d to 9d</li>
 *     <li>Professional: 1p to 9p</li>
 * </ul>
 */
public final class RankOptions {

    private static final int MIN_KYU = 1;
    private static final int MAX_KYU = 30;
    private static final int MIN_DAN = 1;
    private static final int MAX_DAN = 9;
    private static final int MIN_PRO = 1;
    private static final int MAX_PRO = 9;

    private static final Pattern NORMALIZED_RANK_PATTERN = Pattern.compile("^\\d+(k|d|p)$");

    private RankOptions() {
        throw new UnsupportedOperationException("Utility class must not be instantiated.");
    }

    /**
     * Returns all supported Go rank values in display order.
     *
     * <p>The order is:
     * <ul>
     *     <li>30k down to 1k</li>
     *     <li>1d up to 9d</li>
     *     <li>1p up to 9p</li>
     * </ul>
     *
     * @return a list of all supported rank values
     */
    public static List<String> all() {
        List<String> ranks = new ArrayList<>();

        for (int i = MAX_KYU; i >= MIN_KYU; i--) {
            ranks.add(i + "k");
        }

        for (int i = MIN_DAN; i <= MAX_DAN; i++) {
            ranks.add(i + "d");
        }

        for (int i = MIN_PRO; i <= MAX_PRO; i++) {
            ranks.add(i + "p");
        }

        return ranks;
    }

    /**
     * Normalizes a user-provided Go rank into the canonical internal format.
     *
     * <p>Examples:
     * <ul>
     *     <li>"10 kyu" -> "10k"</li>
     *     <li>"1 Dan" -> "1d"</li>
     *     <li>"2PRO" -> "2p"</li>
     * </ul>
     *
     * <p>An empty or blank value is normalized to an empty string.
     *
     * @param value the raw rank value entered by the user
     * @return the normalized rank value, or an empty string if the input is blank
     * @throws IllegalArgumentException if the rank format or range is invalid
     */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace(" ", "");

        normalized = normalized
                .replace("kyu", "k")
                .replace("dan", "d")
                .replace("pro", "p");

        if (!NORMALIZED_RANK_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Ungültiger Rang: " + value);
        }

        int number = Integer.parseInt(normalized.substring(0, normalized.length() - 1));
        String type = normalized.substring(normalized.length() - 1);

        validateRankRange(number, type, value);

        return number + type;
    }

    /**
     * Checks whether a rank value is valid.
     *
     * @param value the rank value to validate
     * @return {@code true} if the value can be normalized successfully, otherwise {@code false}
     */
    public static boolean isValid(String value) {
        try {
            normalize(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates whether the numeric part of a normalized rank is within the
     * allowed range for its rank type.
     *
     * @param number the numeric rank value
     * @param type the normalized rank type ({@code k}, {@code d}, or {@code p})
     * @param originalValue the original input value used for error reporting
     * @throws IllegalArgumentException if the number is outside the allowed range
     */
    private static void validateRankRange(int number, String type, String originalValue) {
        switch (type) {
            case "k" -> {
                if (number < MIN_KYU || number > MAX_KYU) {
                    throw new IllegalArgumentException(
                            "Kyu-Rang muss zwischen 30k und 1k liegen: " + originalValue
                    );
                }
            }
            case "d" -> {
                if (number < MIN_DAN || number > MAX_DAN) {
                    throw new IllegalArgumentException(
                            "Dan-Rang muss zwischen 1d und 9d liegen: " + originalValue
                    );
                }
            }
            case "p" -> {
                if (number < MIN_PRO || number > MAX_PRO) {
                    throw new IllegalArgumentException(
                            "Profi-Rang muss zwischen 1p und 9p liegen: " + originalValue
                    );
                }
            }
            default -> throw new IllegalArgumentException("Ungültiger Rang: " + originalValue);
        }
    }
}