package com.evlaleyla.gotournamentmanager.backend;

import com.evlaleyla.gotournamentmanager.backend.macmahon.TournamentStanding;
import com.evlaleyla.gotournamentmanager.backend.macmahon.TournamentStandingRepository;
import com.evlaleyla.gotournamentmanager.backend.pairing.Pairing;
import com.evlaleyla.gotournamentmanager.backend.pairing.PairingRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class TournamentDataReferenceService {

    private final PairingRepository pairingRepository;
    private final TournamentStandingRepository tournamentStandingRepository;

    public TournamentDataReferenceService(PairingRepository pairingRepository,
                                          TournamentStandingRepository tournamentStandingRepository) {
        this.pairingRepository = pairingRepository;
        this.tournamentStandingRepository = tournamentStandingRepository;
    }

    public boolean hasPairingReferenceForTournamentAndParticipantName(Long tournamentId, String participantName) {
        String normalizedParticipantName = normalizeNameForLookup(participantName);

        if (normalizedParticipantName.isBlank() || tournamentId == null) {
            return false;
        }

        return pairingRepository.findByTournamentIdOrderByRoundNumberAscTableNumberAsc(tournamentId)
                .stream()
                .anyMatch(pairing -> matchesPairing(pairing, normalizedParticipantName));
    }

    public boolean hasStandingReferenceForTournamentAndParticipantName(Long tournamentId, String participantName) {
        String normalizedParticipantName = normalizeNameForLookup(participantName);

        if (normalizedParticipantName.isBlank() || tournamentId == null) {
            return false;
        }

        return tournamentStandingRepository.findByTournamentIdOrderByPlaceAsc(tournamentId)
                .stream()
                .anyMatch(standing -> normalizedParticipantName.equals(normalizeNameForLookup(standing.getName())));
    }

    public boolean hasPairingReferenceForParticipantName(String participantName) {
        String normalizedParticipantName = normalizeNameForLookup(participantName);

        if (normalizedParticipantName.isBlank()) {
            return false;
        }

        return pairingRepository.findAll()
                .stream()
                .anyMatch(pairing -> matchesPairing(pairing, normalizedParticipantName));
    }

    public boolean hasStandingReferenceForParticipantName(String participantName) {
        String normalizedParticipantName = normalizeNameForLookup(participantName);

        if (normalizedParticipantName.isBlank()) {
            return false;
        }

        return tournamentStandingRepository.findAll()
                .stream()
                .map(TournamentStanding::getName)
                .anyMatch(name -> normalizedParticipantName.equals(normalizeNameForLookup(name)));
    }

    private boolean matchesPairing(Pairing pairing, String normalizedParticipantName) {
        return normalizedParticipantName.equals(normalizeNameForLookup(pairing.getBlackPlayer()))
                || normalizedParticipantName.equals(normalizeNameForLookup(pairing.getWhitePlayer()));
    }

    private String normalizeNameForLookup(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().replaceAll("\\s+", " ");

        if (normalized.contains(",")) {
            String[] parts = normalized.split(",", 2);
            String lastName = parts[0].trim();
            String firstName = parts[1].trim();
            normalized = (firstName + " " + lastName).trim();
        }

        return normalized.toLowerCase(Locale.ROOT);
    }
}