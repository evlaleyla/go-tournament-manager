package com.evlaleyla.gotournamentmanager.backend.tournament;

public enum TournamentStatus {
    PLANNED("Geplant"),
    REGISTRATION_OPEN("Anmeldung offen"),
    RUNNING("Laufend"),
    FINISHED("Abgeschlossen");

    private final String displayName;

    TournamentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}