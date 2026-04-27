package com.evlaleyla.gotournamentmanager.backend;

/**
 * Simple immutable value object representing a selectable country option.
 *
 * <p>A country option consists of a normalized country code and its
 * human-readable display name.</p>
 */
public class CountryOption {

    /**
     * Normalized country code, for example {@code "de"} or {@code "fr"}.
     */
    private final String code;

    /**
     * Human-readable country name shown in the user interface.
     */
    private final String name;

    /**
     * Creates a new country option.
     *
     * @param code the normalized country code
     * @param name the display name of the country
     */
    public CountryOption(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * Returns the normalized country code.
     *
     * @return the country code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the display name of the country.
     *
     * @return the human-readable country name
     */
    public String getName() {
        return name;
    }
}