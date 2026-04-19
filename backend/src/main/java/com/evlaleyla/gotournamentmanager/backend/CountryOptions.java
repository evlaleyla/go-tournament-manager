package com.evlaleyla.gotournamentmanager.backend;

import java.util.List;
import java.util.Locale;

public final class CountryOptions {

    private CountryOptions() {
    }

    public static List<CountryOption> all() {
        return List.of(
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
    }

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

    public static boolean exists(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        String normalized = code.trim().toLowerCase(Locale.ROOT);

        return all().stream()
                .anyMatch(country -> country.getCode().equals(normalized));
    }

    public static String displayName(String value) {
        String normalized = normalize(value);

        return all().stream()
                .filter(country -> country.getCode().equals(normalized))
                .map(CountryOption::getName)
                .findFirst()
                .orElse(normalized);
    }
}