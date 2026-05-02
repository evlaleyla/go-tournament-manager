package com.evlaleyla.gotournamentmanager.backend;

import java.util.List;
import java.util.Map;

/**
 * Provides predefined club options grouped by country and helper methods
 * for club lookup, normalization, and validation.
 *
 * <p>This class is designed as a utility class and therefore cannot be instantiated.</p>
 */
public final class ClubOptions {

    /**
     * Predefined club names grouped by normalized country code.
     *
     * <p>The country codes are expected to match the normalized values returned
     * by {@link CountryOptions#normalize(String)}.</p>
     */
    private static final Map<String, List<String>> CLUBS_BY_COUNTRY = Map.ofEntries(
            Map.entry("de", List.of(
                    "Aachen",
                    "Berlin",
                    "Bielefeld",
                    "Bonn",
                    "Dortmund",
                    "Dresden",
                    "Düsseldorf",
                    "Frankfurt",
                    "Hamburg",
                    "Hannover",
                    "Heidelberg",
                    "Karlsruhe",
                    "Köln",
                    "Leipzig",
                    "Mannheim",
                    "München",
                    "Stuttgart"
            )),
            Map.entry("at", List.of(
                    "Graz",
                    "Innsbruck",
                    "Linz",
                    "Salzburg",
                    "Wien"
            )),
            Map.entry("ch", List.of(
                    "Basel",
                    "Bern",
                    "Genf",
                    "Lausanne",
                    "Zürich"
            )),
            Map.entry("fr", List.of(
                    "Lyon",
                    "Marseille",
                    "Paris",
                    "Toulouse"
            )),
            Map.entry("it", List.of(
                    "Milano",
                    "Roma",
                    "Torino"
            )),
            Map.entry("es", List.of(
                    "Barcelona",
                    "Madrid",
                    "Valencia"
            )),
            Map.entry("nl", List.of(
                    "Amsterdam",
                    "Den Haag",
                    "Eindhoven",
                    "Rotterdam"
            )),
            Map.entry("jp", List.of(
                    "Osaka",
                    "Tokyo"
            )),
            Map.entry("kr", List.of(
                    "Busan",
                    "Seoul"
            )),
            Map.entry("cn", List.of(
                    "Beijing",
                    "Shanghai"
            )),
            Map.entry("us", List.of(
                    "Boston",
                    "Chicago",
                    "New York",
                    "San Francisco"
            )),
            Map.entry("gb", List.of(
                    "Cambridge",
                    "London",
                    "Oxford"
            ))
    );

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ClubOptions() {
    }

    /**
     * Returns all configured clubs for the given country.
     *
     * <p>The provided country value is normalized first. If the normalized
     * country is blank or no clubs are configured for it, an empty list is returned.</p>
     *
     * @param countryCode the raw or normalized country code
     * @return the list of clubs for the country, or an empty list if none are available
     * @throws IllegalArgumentException if the given country code is invalid
     */
    public static List<String> findByCountry(String countryCode) {
        String normalizedCountry = CountryOptions.normalize(countryCode);

        if (normalizedCountry.isBlank()) {
            return List.of();
        }

        return CLUBS_BY_COUNTRY.getOrDefault(normalizedCountry, List.of());
    }

    /**
     * Returns a flattened, distinct, and alphabetically sorted list of all configured clubs.
     *
     * @return all configured club names across all countries
     */
    public static List<String> all() {
        return CLUBS_BY_COUNTRY.values()
                .stream()
                .flatMap(List::stream)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Returns the complete country-to-club mapping.
     *
     * @return the predefined club mapping grouped by country code
     */
    public static Map<String, List<String>> byCountry() {
        return CLUBS_BY_COUNTRY;
    }

    /**
     * Normalizes a club value for consistent storage and comparison.
     *
     * <p>This removes forbidden separator characters, replaces line breaks,
     * collapses repeated whitespace, and trims the result.</p>
     *
     * @param club the raw club value
     * @return the normalized club value, or an empty string if the input is null or blank
     */
    public static String normalize(String club) {
        if (club == null || club.isBlank()) {
            return "";
        }

        return club
                .replace("|", " ")
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Checks whether a club value is valid for a given country.
     *
     * <p>Validation rules:</p>
     * <ul>
     *     <li>An empty club is always considered valid.</li>
     *     <li>A non-empty club without a valid country is invalid.</li>
     *     <li>If no club list is configured for a country, any normalized club is accepted.</li>
     *     <li>If a club list exists, the club must match one configured entry case-insensitively.</li>
     * </ul>
     *
     * @param countryCode the raw or normalized country code
     * @param club the raw club value
     * @return {@code true} if the club is valid for the country, otherwise {@code false}
     * @throws IllegalArgumentException if the given country code is invalid
     */
    public static boolean isValidForCountry(String countryCode, String club) {
        String normalizedClub = normalize(club);

        if (normalizedClub.isBlank()) {
            return true;
        }

        String normalizedCountry = CountryOptions.normalize(countryCode);

        if (normalizedCountry.isBlank()) {
            return false;
        }

        List<String> clubsForCountry = CLUBS_BY_COUNTRY.get(normalizedCountry);

        if (clubsForCountry == null || clubsForCountry.isEmpty()) {
            return true;
        }

        return clubsForCountry.stream()
                .anyMatch(allowedClub -> allowedClub.equalsIgnoreCase(normalizedClub));
    }
}