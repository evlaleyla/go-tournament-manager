package com.evlaleyla.gotournamentmanager.backend.tournament;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable view model for the tournament dashboard.
 *
 * <p>This class bundles aggregated tournament information that is displayed
 * on the tournament detail page. It contains both raw values and small
 * presentation helper methods for labels shown in the UI.</p>
 */
public class TournamentDashboardData {

    /**
     * Formatter used for displaying date-time values in the German UI.
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Total number of registrations for the tournament.
     */
    private final long totalRegistrations;

    /**
     * Number of participants eligible to play in round one.
     */
    private final long eligibleInRoundOne;

    /**
     * Current round derived for dashboard display.
     */
    private final int currentRound;

    /**
     * Number of participants eligible to play in the currently relevant round.
     */
    private final long eligibleInCurrentRound;

    /**
     * Number of rounds for which pairings have already been imported.
     */
    private final long importedPairingRoundCount;

    /**
     * Highest round number for which pairings have been imported.
     * May be {@code null} if no pairing import exists yet.
     */
    private final Integer lastImportedPairingRound;

    /**
     * Timestamp of the most recent wall list import.
     * May be {@code null} if no wall list has been imported yet.
     */
    private final LocalDateTime lastWallListImportAt;

    /**
     * Creates a new dashboard data object.
     *
     * @param totalRegistrations        total number of registrations
     * @param eligibleInRoundOne        number of participants eligible in round one
     * @param currentRound              current round for dashboard display
     * @param eligibleInCurrentRound    number of participants eligible in the current round
     * @param importedPairingRoundCount number of rounds with imported pairings
     * @param lastImportedPairingRound  highest imported pairing round, may be {@code null}
     * @param lastWallListImportAt      timestamp of the latest wall list import, may be {@code null}
     */
    public TournamentDashboardData(long totalRegistrations,
                                   long eligibleInRoundOne,
                                   int currentRound,
                                   long eligibleInCurrentRound,
                                   long importedPairingRoundCount,
                                   Integer lastImportedPairingRound,
                                   LocalDateTime lastWallListImportAt) {
        this.totalRegistrations = totalRegistrations;
        this.eligibleInRoundOne = eligibleInRoundOne;
        this.currentRound = currentRound;
        this.eligibleInCurrentRound = eligibleInCurrentRound;
        this.importedPairingRoundCount = importedPairingRoundCount;
        this.lastImportedPairingRound = lastImportedPairingRound;
        this.lastWallListImportAt = lastWallListImportAt;
    }

    /**
     * @return total number of registrations
     */
    public long getTotalRegistrations() {
        return totalRegistrations;
    }

    /**
     * @return number of participants eligible in round one
     */
    public long getEligibleInRoundOne() {
        return eligibleInRoundOne;
    }

    /**
     * @return current round for dashboard display
     */
    public int getCurrentRound() {
        return currentRound;
    }

    /**
     * @return number of participants eligible in the current round
     */
    public long getEligibleInCurrentRound() {
        return eligibleInCurrentRound;
    }

    /**
     * @return number of rounds with imported pairings
     */
    public long getImportedPairingRoundCount() {
        return importedPairingRoundCount;
    }

    /**
     * @return highest imported pairing round, or {@code null} if none exists
     */
    public Integer getLastImportedPairingRound() {
        return lastImportedPairingRound;
    }

    /**
     * @return timestamp of the most recent wall list import, or {@code null} if none exists
     */
    public LocalDateTime getLastWallListImportAt() {
        return lastWallListImportAt;
    }

    /**
     * Returns a UI-ready label for the current round.
     *
     * @return formatted current round label
     */
    public String getCurrentRoundLabel() {
        return "Runde " + currentRound;
    }

    /**
     * Returns a UI-ready label for the most recently imported pairing round.
     *
     * @return formatted round label or fallback text if no import exists
     */
    public String getLastImportedPairingRoundDisplay() {
        if (lastImportedPairingRound == null) {
            return "Noch kein Import";
        }

        return "Runde " + lastImportedPairingRound;
    }

    /**
     * Returns a UI-ready timestamp for the last wall list import.
     *
     * @return formatted timestamp or fallback text if no import exists
     */
    public String getLastWallListImportDisplay() {
        if (lastWallListImportAt == null) {
            return "Noch kein Import";
        }

        return lastWallListImportAt.format(DATE_TIME_FORMATTER);
    }
}