package com.evlaleyla.gotournamentmanager.backend.macmahon;

/**
 * Represents a single pairing row imported from a MacMahon export file.
 *
 * <p>This record is used as a simple immutable data carrier between the
 * parsing layer and the pairing import logic.</p>
 *
 * @param roundNumber the round number the pairing belongs to
 * @param tableNumber the table number within the round
 * @param blackPlayer the name of the player assigned to black
 * @param whitePlayer the name of the player assigned to white
 * @param result the normalized internal result code, for example {@code B}, {@code W}, {@code J},
 *               {@code L}, {@code D}, or {@code null} if no result is available yet
 * @param handicap the handicap value imported from MacMahon, or {@code null} if none is present
 * @param bye indicates whether this row represents a bye pairing
 */
public record MacMahonPairingImportRow(
        Integer roundNumber,
        Integer tableNumber,
        String blackPlayer,
        String whitePlayer,
        String result,
        String handicap,
        boolean bye
) {
}