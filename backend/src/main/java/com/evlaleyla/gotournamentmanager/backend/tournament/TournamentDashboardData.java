package com.evlaleyla.gotournamentmanager.backend.tournament;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TournamentDashboardData {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final long totalRegistrations;
    private final long eligibleInRoundOne;
    private final int currentRound;
    private final long eligibleInCurrentRound;
    private final long importedPairingRoundCount;
    private final Integer lastImportedPairingRound;
    private final LocalDateTime lastWallListImportAt;

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

    public long getTotalRegistrations() {
        return totalRegistrations;
    }

    public long getEligibleInRoundOne() {
        return eligibleInRoundOne;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public long getEligibleInCurrentRound() {
        return eligibleInCurrentRound;
    }

    public long getImportedPairingRoundCount() {
        return importedPairingRoundCount;
    }

    public Integer getLastImportedPairingRound() {
        return lastImportedPairingRound;
    }

    public LocalDateTime getLastWallListImportAt() {
        return lastWallListImportAt;
    }

    public String getCurrentRoundLabel() {
        return "Runde " + currentRound;
    }

    public String getLastImportedPairingRoundDisplay() {
        if (lastImportedPairingRound == null) {
            return "Noch kein Import";
        }
        return "Runde " + lastImportedPairingRound;
    }

    public String getLastWallListImportDisplay() {
        if (lastWallListImportAt == null) {
            return "Noch kein Import";
        }
        return lastWallListImportAt.format(DATE_TIME_FORMATTER);
    }
}