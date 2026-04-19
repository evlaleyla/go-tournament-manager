package com.evlaleyla.gotournamentmanager.backend;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RankOptions {

    private RankOptions() {
        // Utility-Klasse: soll nicht instanziiert werden
    }

    public static List<String> all() {
        List<String> ranks = new ArrayList<>();

        for (int i = 30; i >= 1; i--) {
            ranks.add(i + "k");
        }

        for (int i = 1; i <= 9; i++) {
            ranks.add(i + "d");
        }

        for (int i = 1; i <= 9; i++) {
            ranks.add(i + "p");
        }

        return ranks;
    }

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

        if (!normalized.matches("^\\d+(k|d|p)$")) {
            throw new IllegalArgumentException("Ungültiger Rang: " + value);
        }

        int number = Integer.parseInt(normalized.substring(0, normalized.length() - 1));
        String type = normalized.substring(normalized.length() - 1);

        switch (type) {
            case "k" -> {
                if (number < 1 || number > 30) {
                    throw new IllegalArgumentException("Kyu-Rang muss zwischen 30k und 1k liegen: " + value);
                }
            }
            case "d" -> {
                if (number < 1 || number > 9) {
                    throw new IllegalArgumentException("Dan-Rang muss zwischen 1d und 9d liegen: " + value);
                }
            }
            case "p" -> {
                if (number < 1 || number > 9) {
                    throw new IllegalArgumentException("Profi-Rang muss zwischen 1p und 9p liegen: " + value);
                }
            }
            default -> throw new IllegalArgumentException("Ungültiger Rang: " + value);
        }

        return number + type;
    }

    public static boolean isValid(String value) {
        try {
            normalize(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}