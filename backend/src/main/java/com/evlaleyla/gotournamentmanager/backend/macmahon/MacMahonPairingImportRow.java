package com.evlaleyla.gotournamentmanager.backend.macmahon;

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