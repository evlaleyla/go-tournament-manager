package com.evlaleyla.gotournamentmanager.backend;

public class CountryOption {

    private final String code;
    private final String name;

    public CountryOption(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}