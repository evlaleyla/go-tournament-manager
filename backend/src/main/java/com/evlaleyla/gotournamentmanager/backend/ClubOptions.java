package com.evlaleyla.gotournamentmanager.backend;

import java.util.List;
import java.util.Map;

public final class ClubOptions {

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

    private ClubOptions() {
    }

    public static List<String> findByCountry(String countryCode) {
        String normalizedCountry = CountryOptions.normalize(countryCode);

        if (normalizedCountry.isBlank()) {
            return List.of();
        }

        return CLUBS_BY_COUNTRY.getOrDefault(normalizedCountry, List.of());
    }

    public static List<String> all() {
        return CLUBS_BY_COUNTRY.values()
                .stream()
                .flatMap(List::stream)
                .distinct()
                .sorted()
                .toList();
    }

    public static Map<String, List<String>> byCountry() {
        return CLUBS_BY_COUNTRY;
    }

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