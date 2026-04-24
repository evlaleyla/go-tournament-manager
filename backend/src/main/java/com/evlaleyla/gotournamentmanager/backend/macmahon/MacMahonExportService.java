package com.evlaleyla.gotournamentmanager.backend.macmahon;

import com.evlaleyla.gotournamentmanager.backend.ClubOptions;
import com.evlaleyla.gotournamentmanager.backend.CountryOptions;
import com.evlaleyla.gotournamentmanager.backend.RankOptions;
import com.evlaleyla.gotournamentmanager.backend.participant.Participant;
import com.evlaleyla.gotournamentmanager.backend.registration.Registration;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationService;
import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class MacMahonExportService {

    private final TournamentService tournamentService;
    private final RegistrationService registrationService;

    public MacMahonExportService(TournamentService tournamentService,
                                 RegistrationService registrationService) {
        this.tournamentService = tournamentService;
        this.registrationService = registrationService;
    }

    public byte[] exportParticipantsForMacMahon(Long tournamentId) {
        Tournament tournament = tournamentService.findById(tournamentId);
        List<Registration> registrations =
                registrationService.findStartListByTournamentId(tournamentId);

        if (registrations.isEmpty()) {
            throw new IllegalArgumentException("Für dieses Turnier gibt es keine Anmeldungen.");
        }

        String nl = "\r\n";
        StringBuilder out = new StringBuilder();

        out.append("; MacMahon-Import für ")
                .append(sanitize(tournament.getName()))
                .append(nl);

        out.append("; surname|firstname|strength|country|club|rating|registration|playinginrounds")
                .append(nl);

        int totalRounds = tournament.getNumberOfRounds();

        validateUniqueParticipantNamesForMacMahon(registrations);

        for (Registration registration : registrations) {
            Participant participant = registration.getParticipant();

            String participantName = buildParticipantName(participant);

            String surname = sanitize(participant.getLastName());
            if (surname.isBlank()) {
                throw new IllegalArgumentException(
                        "Teilnehmer ohne Nachname kann nicht nach MacMahon exportiert werden: " + participantName
                );
            }

            String firstname = sanitize(participant.getFirstName());
            String strength = normalizeRankForExport(registration.getRankAtRegistration(), participantName);
            String country = normalizeCountryForExport(registration.getCountryAtRegistration(), participantName);
            String club = normalizeClubForExport(
                    registration.getClubAtRegistration(),
                    country,
                    participantName
            );
            String rating = "";
            String registrationFlag = "f";
            String playingInRounds = buildPlayingInRounds(
                    registration.getPlannedRounds(),
                    totalRounds
            );

            out.append(String.join("|",
                    surname,
                    firstname,
                    strength,
                    country,
                    club,
                    rating,
                    registrationFlag,
                    playingInRounds
            )).append(nl);
        }

        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String buildParticipantName(Participant participant) {
        String firstName = participant.getFirstName() == null
                ? ""
                : participant.getFirstName().trim();

        String lastName = participant.getLastName() == null
                ? ""
                : participant.getLastName().trim();

        String fullName = (firstName + " " + lastName).trim();

        if (fullName.isBlank()) {
            return "unbekannter Teilnehmer";
        }

        return fullName;
    }

    private String normalizeRankForExport(String rank, String participantName) {
        try {
            return RankOptions.normalize(rank);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Ungültiger Rang für MacMahon-Export bei Teilnehmer '" +
                            participantName + "': " + rank
            );
        }
    }

    private String normalizeCountryForExport(String country, String participantName) {
        try {
            return CountryOptions.normalize(country);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Ungültiges Land für MacMahon-Export bei Teilnehmer '" +
                            participantName + "': " + country
            );
        }
    }

    private String buildPlayingInRounds(Integer plannedRounds, Integer totalRounds) {
        if (plannedRounds == null || totalRounds == null || totalRounds < 1) {
            return "";
        }

        int roundsToPlay = Math.max(0, Math.min(plannedRounds, totalRounds));
        StringBuilder sb = new StringBuilder(totalRounds);

        for (int i = 0; i < totalRounds; i++) {
            sb.append(i < roundsToPlay ? '1' : '0');
        }

        return sb.toString();
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("|", " ")
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeClubForExport(String club, String country, String participantName) {
        String normalizedClub = ClubOptions.normalize(club);

        if (!ClubOptions.isValidForCountry(country, normalizedClub)) {
            throw new IllegalArgumentException(
                    "Ungültiger Verein für MacMahon-Export bei Teilnehmer '" +
                            participantName + "': " + normalizedClub +
                            " passt nicht zum Land " + country
            );
        }

        return normalizedClub;
    }

    private void validateUniqueParticipantNamesForMacMahon(List<Registration> registrations) {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        java.util.Map<String, String> displayNames = new java.util.HashMap<>();

        for (Registration registration : registrations) {
            Participant participant = registration.getParticipant();

            String displayName = buildReadableFullName(
                    participant.getFirstName(),
                    participant.getLastName()
            );

            String normalizedName = normalizeFullName(
                    participant.getFirstName(),
                    participant.getLastName()
            );

            if (normalizedName.isBlank()) {
                throw new IllegalArgumentException(
                        "Ein Teilnehmer hat keinen eindeutig nutzbaren Namen für den MacMahon-Export."
                );
            }

            counts.put(normalizedName, counts.getOrDefault(normalizedName, 0) + 1);
            displayNames.putIfAbsent(normalizedName, displayName);
        }

        List<String> duplicateNames = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> displayNames.get(entry.getKey()))
                .sorted()
                .toList();

        if (!duplicateNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "Der MacMahon-Export ist nicht eindeutig möglich, da folgende Namen im Turnier mehrfach vorkommen: "
                            + String.join(", ", duplicateNames)
                            + ". Bitte diese Teilnehmenden vor dem Export manuell unterscheiden."
            );
        }
    }
    
    private String buildReadableFullName(String firstName, String lastName) {
        String readableFirstName = firstName == null ? "" : firstName.trim();
        String readableLastName = lastName == null ? "" : lastName.trim();

        return (readableFirstName + " " + readableLastName)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String normalizeFullName(String firstName, String lastName) {
        String normalizedFirstName = firstName == null ? "" : firstName.trim();
        String normalizedLastName = lastName == null ? "" : lastName.trim();

        return (normalizedFirstName + " " + normalizedLastName)
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(java.util.Locale.ROOT);
    }
}