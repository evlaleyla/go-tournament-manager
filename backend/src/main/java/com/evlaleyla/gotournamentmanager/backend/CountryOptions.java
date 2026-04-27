package com.evlaleyla.gotournamentmanager.backend;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class providing supported country options and helper methods
 * for normalization, validation and display-name lookup.
 *
 * <p>The class centralizes all country-related mappings used by the application
 * so that country codes are handled consistently across forms, services and exports.</p>
 */
public final class CountryOptions {

    /**
     * Immutable list of all supported country options.
     */
    private static final List<CountryOption> COUNTRY_OPTIONS = List.of(
            new CountryOption("de", "Deutschland"),
            new CountryOption("at", "Österreich"),
            new CountryOption("ch", "Schweiz"),
            new CountryOption("fr", "Frankreich"),
            new CountryOption("it", "Italien"),
            new CountryOption("es", "Spanien"),
            new CountryOption("nl", "Niederlande"),
            new CountryOption("be", "Belgien"),
            new CountryOption("pl", "Polen"),
            new CountryOption("cz", "Tschechien"),
            new CountryOption("dk", "Dänemark"),
            new CountryOption("se", "Schweden"),
            new CountryOption("no", "Norwegen"),
            new CountryOption("fi", "Finnland"),
            new CountryOption("gb", "Großbritannien"),
            new CountryOption("ie", "Irland"),
            new CountryOption("pt", "Portugal"),
            new CountryOption("gr", "Griechenland"),
            new CountryOption("hu", "Ungarn"),
            new CountryOption("ro", "Rumänien"),
            new CountryOption("jp", "Japan"),
            new CountryOption("kr", "Südkorea"),
            new CountryOption("cn", "China"),
            new CountryOption("tw", "Taiwan"),
            new CountryOption("us", "USA"),
            new CountryOption("ca", "Kanada"),
            new CountryOption("br", "Brasilien"),
            new CountryOption("au", "Australien"),
            new CountryOption("nz", "Neuseeland")
    );

    /**
     * Lookup map for resolving normalized country codes to display names.
     */
    private static final Map<String, String> DISPLAY_NAMES_BY_CODE = COUNTRY_OPTIONS.stream()
            .collect(Collectors.toUnmodifiableMap(CountryOption::getCode, CountryOption::getName));

    /**
     * Set of all supported normalized country codes.
     */
    private static final Set<String> SUPPORTED_CODES = DISPLAY_NAMES_BY_CODE.keySet();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private CountryOptions() {
        throw new AssertionError("Utility class must not be instantiated.");
    }

    /**
     * Returns all supported country options.
     *
     * @return immutable list of supported countries
     */
    public static List<CountryOption> all() {
        return COUNTRY_OPTIONS;
    }

    /**
     * Normalizes a user-provided country value to the application's internal
     * two-letter country code.
     *
     * <p>Supported inputs include normalized codes as well as selected
     * German and English country names and common aliases.</p>
     *
     * @param value raw country input
     * @return normalized two-letter country code, or an empty string if the input is blank
     * @throws IllegalArgumentException if the input cannot be mapped to a supported country
     */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "de", "deutschland", "germany" -> "de";
            case "at", "österreich", "oesterreich", "austria" -> "at";
            case "ch", "schweiz", "switzerland" -> "ch";
            case "fr", "frankreich", "france" -> "fr";
            case "it", "italien", "italy" -> "it";
            case "es", "spanien", "spain" -> "es";
            case "nl", "niederlande", "netherlands" -> "nl";
            case "jp", "japan" -> "jp";
            case "kr", "korea", "südkorea", "suedkorea", "south korea" -> "kr";
            case "cn", "china" -> "cn";
            case "us", "usa", "united states", "vereinigte staaten" -> "us";
            case "gb", "uk", "großbritannien", "grossbritannien", "united kingdom" -> "gb";
            default -> {
                if (normalized.matches("^[a-z]{2}$") && exists(normalized)) {
                    yield normalized;
                }

                throw new IllegalArgumentException("Ungültiger Ländercode: " + value);
            }
        };
    }

    /**
     * Checks whether a normalized country code is supported by the application.
     *
     * @param code country code to check
     * @return {@code true} if the code is supported, otherwise {@code false}
     */
    public static boolean exists(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        String normalized = code.trim().toLowerCase(Locale.ROOT);
        return SUPPORTED_CODES.contains(normalized);
    }

    /**
     * Resolves a country value to its display name.
     *
     * <p>The input may be either a country code or a supported country alias.
     * Internally, the value is normalized first.</p>
     *
     * @param value country code or alias
     * @return human-readable country name
     * @throws IllegalArgumentException if the value cannot be normalized
     */
    public static String displayName(String value) {
        String normalized = normalize(value);
        return DISPLAY_NAMES_BY_CODE.getOrDefault(normalized, normalized);
    }
}