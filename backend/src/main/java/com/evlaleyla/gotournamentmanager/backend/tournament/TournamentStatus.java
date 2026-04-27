package com.evlaleyla.gotournamentmanager.backend.tournament;

/**
 * Defines the lifecycle states of a tournament.
 *
 * <p>The enum values are used internally in the application logic,
 * while the German display names are intended for presentation in the UI.</p>
 */
public enum TournamentStatus {

    /**
     * The tournament exists, but registration has not opened yet.
     */
    PLANNED("Geplant"),

    /**
     * The tournament is open for participant registration.
     */
    REGISTRATION_OPEN("Anmeldung offen"),

    /**
     * The tournament is currently being conducted.
     */
    RUNNING("Laufend"),

    /**
     * The tournament has been completed.
     */
    FINISHED("Abgeschlossen");

    /**
     * Human-readable label used in the German user interface.
     */
    private final String displayName;

    /**
     * Creates a tournament status with its UI display name.
     *
     * @param displayName the human-readable German label
     */
    TournamentStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable label for UI output.
     *
     * @return the German display name of the status
     */
    public String getDisplayName() {
        return displayName;
    }
}