package com.evlaleyla.gotournamentmanager.backend.macmahon;

import com.evlaleyla.gotournamentmanager.backend.ClubOptions;
import com.evlaleyla.gotournamentmanager.backend.CountryOptions;
import com.evlaleyla.gotournamentmanager.backend.RankOptions;
import com.evlaleyla.gotournamentmanager.backend.participant.Participant;
import com.evlaleyla.gotournamentmanager.backend.registration.Registration;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationService;
import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Service responsible for exporting tournament participant data
 * into a MacMahon-compatible text format.
 *
 * <p>The export contains the registered players of a tournament and
 * transforms internal domain data into the format expected by MacMahon.
 * Validation is performed before export to ensure that the generated file
 * is unambiguous and semantically valid.</p>
 */
@Service
public class MacMahonExportService {

    private static final Logger log = LoggerFactory.getLogger(MacMahonExportService.class);

    private static final String LINE_SEPARATOR = "\r\n";
    private static final String REGISTRATION_FLAG = "f";
    private static final String EMPTY_RATING = "";

    private final TournamentService tournamentService;
    private final RegistrationService registrationService;

    public MacMahonExportService(TournamentService tournamentService,
                                 RegistrationService registrationService) {
        this.tournamentService = tournamentService;
        this.registrationService = registrationService;
    }

    /**
     * Exports all registrations of the given tournament to a MacMahon-compatible file.
     *
     * <p>The method validates:
     * <ul>
     *     <li>that registrations exist,</li>
     *     <li>that participant names are unique for MacMahon matching,</li>
     *     <li>that rank, country, and club data can be exported consistently.</li>
     * </ul>
     *
     * @param tournamentId the ID of the tournament to export
     * @return the export content as UTF-8 encoded bytes
     * @throws IllegalArgumentException if the export cannot be created safely
     */
    public byte[] exportParticipantsForMacMahon(Long tournamentId) {
        log.info("Starting MacMahon participant export for tournamentId={}", tournamentId);

        Tournament tournament = tournamentService.findById(tournamentId);
        List<Registration> registrations = registrationService.findStartListByTournamentId(tournamentId);

        if (registrations.isEmpty()) {
            log.warn("MacMahon export aborted because no registrations were found for tournamentId={}", tournamentId);
            throw new IllegalArgumentException("Für dieses Turnier gibt es keine Anmeldungen.");
        }

        log.debug(
                "Loaded {} registrations for MacMahon export of tournamentId={}, tournamentName={}",
                registrations.size(),
                tournamentId,
                tournament.getName()
        );

        validateUniqueParticipantNamesForMacMahon(registrations);

        StringBuilder exportContent = new StringBuilder();
        appendHeader(exportContent, tournament);

        Integer totalRounds = tournament.getNumberOfRounds();

        for (Registration registration : registrations) {
            exportContent.append(buildExportLine(registration, totalRounds)).append(LINE_SEPARATOR);
        }

        byte[] result = exportContent.toString().getBytes(StandardCharsets.UTF_8);

        log.info(
                "Successfully finished MacMahon participant export for tournamentId={} with {} registrations",
                tournamentId,
                registrations.size()
        );

        return result;
    }

    /**
     * Appends the MacMahon file header.
     */
    private void appendHeader(StringBuilder exportContent, Tournament tournament) {
        exportContent.append("; MacMahon-Import for ")
                .append(sanitize(tournament.getName()))
                .append(LINE_SEPARATOR);

        exportContent.append("; surname|firstname|strength|country|club|rating|registration|playinginrounds")
                .append(LINE_SEPARATOR);
    }

    /**
     * Builds a single MacMahon export line for one registration.
     */
    private String buildExportLine(Registration registration, Integer totalRounds) {
        Participant participant = registration.getParticipant();
        String participantName = buildParticipantDisplayName(participant);

        String surname = sanitize(participant.getLastName());
        validateRequiredSurname(surname, participantName);

        String firstName = sanitize(participant.getFirstName());
        String strength = normalizeRankForExport(registration.getRankAtRegistration(), participantName);
        String country = normalizeCountryForExport(registration.getCountryAtRegistration(), participantName);
        String club = normalizeClubForExport(registration.getClubAtRegistration(), country, participantName);
        String playingInRounds = buildPlayingInRoundsField(registration.getSelectedRounds(), totalRounds);

        return String.join("|",
                surname,
                firstName,
                strength,
                country,
                club,
                EMPTY_RATING,
                REGISTRATION_FLAG,
                playingInRounds
        );
    }

    /**
     * Ensures that a participant has a usable surname for MacMahon export.
     *
     * <p>MacMahon relies on name-based identification, so a missing surname
     * would produce ambiguous or unusable export data.</p>
     */
    private void validateRequiredSurname(String surname, String participantName) {
        if (surname.isBlank()) {
            log.warn(
                    "MacMahon export failed because participant has no surname. participantName={}",
                    participantName
            );
            throw new IllegalArgumentException(
                    "Teilnehmer ohne Nachname kann nicht nach MacMahon exportiert werden: " + participantName
            );
        }
    }

    /**
     * Builds a human-readable participant name for logs and validation messages.
     */
    private String buildParticipantDisplayName(Participant participant) {
        String firstName = participant.getFirstName() == null ? "" : participant.getFirstName().trim();
        String lastName = participant.getLastName() == null ? "" : participant.getLastName().trim();

        String fullName = (firstName + " " + lastName).trim();

        if (fullName.isBlank()) {
            return "unknown participant";
        }

        return fullName;
    }

    /**
     * Normalizes and validates the participant rank for MacMahon export.
     */
    private String normalizeRankForExport(String rank, String participantName) {
        try {
            return RankOptions.normalize(rank);
        } catch (IllegalArgumentException e) {
            log.warn(
                    "Invalid rank for MacMahon export. participantName={}, rank={}",
                    participantName,
                    rank
            );
            throw new IllegalArgumentException(
                    "Ungültiger Rang für MacMahon-Export bei Teilnehmer '" +
                            participantName + "': " + rank
            );
        }
    }

    /**
     * Normalizes and validates the participant country for MacMahon export.
     */
    private String normalizeCountryForExport(String country, String participantName) {
        try {
            return CountryOptions.normalize(country);
        } catch (IllegalArgumentException e) {
            log.warn(
                    "Invalid country for MacMahon export. participantName={}, country={}",
                    participantName,
                    country
            );
            throw new IllegalArgumentException(
                    "Ungültiges Land für MacMahon-Export bei Teilnehmer '" +
                            participantName + "': " + country
            );
        }
    }

    /**
     * Normalizes and validates the participant club for MacMahon export.
     *
     * <p>The club must be compatible with the selected country to avoid
     * semantically inconsistent export data.</p>
     */
    private String normalizeClubForExport(String club, String country, String participantName) {
        String normalizedClub = ClubOptions.normalize(club);

        if (!ClubOptions.isValidForCountry(country, normalizedClub)) {
            log.warn(
                    "Invalid club-country combination for MacMahon export. participantName={}, club={}, country={}",
                    participantName,
                    normalizedClub,
                    country
            );
            throw new IllegalArgumentException(
                    "Ungültiger Verein für MacMahon-Export bei Teilnehmer '" +
                            participantName + "': " + normalizedClub +
                            " passt nicht zum Land " + country
            );
        }

        return normalizedClub;
    }

    /**
     * Converts the selected round set into the MacMahon playing-in-rounds field.
     *
     * <p>Example: for 5 rounds and participation in rounds 1, 3 and 5,
     * the result would be {@code 10101}.</p>
     */
    private String buildPlayingInRoundsField(Set<Integer> selectedRounds, Integer totalRounds) {
        if (selectedRounds == null || selectedRounds.isEmpty() || totalRounds == null || totalRounds < 1) {
            return "";
        }

        StringBuilder field = new StringBuilder(totalRounds);

        for (int round = 1; round <= totalRounds; round++) {
            field.append(selectedRounds.contains(round) ? '1' : '0');
        }

        return field.toString();
    }

    /**
     * Sanitizes a value for safe inclusion in the pipe-separated export format.
     *
     * <p>This removes line breaks, pipe characters, and repeated whitespace.</p>
     */
    private String sanitize(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("|", " ")
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Validates that each participant name occurs only once in the export scope.
     *
     * <p>The import/export process relies on names being unique enough for
     * safe matching across systems. Duplicate names would make the MacMahon
     * exchange ambiguous.</p>
     */
    private void validateUniqueParticipantNamesForMacMahon(List<Registration> registrations) {
        Map<String, Integer> countsByNormalizedName = new HashMap<>();
        Map<String, String> displayNameByNormalizedName = new HashMap<>();

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
                log.warn("MacMahon export failed because a participant has no usable full name");
                throw new IllegalArgumentException(
                        "Ein Teilnehmer hat keinen eindeutig nutzbaren Namen für den MacMahon-Export."
                );
            }

            countsByNormalizedName.put(
                    normalizedName,
                    countsByNormalizedName.getOrDefault(normalizedName, 0) + 1
            );

            displayNameByNormalizedName.putIfAbsent(normalizedName, displayName);
        }

        List<String> duplicateNames = countsByNormalizedName.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> displayNameByNormalizedName.get(entry.getKey()))
                .sorted()
                .toList();

        if (!duplicateNames.isEmpty()) {
            log.warn(
                    "MacMahon export failed because duplicate participant names were found: {}",
                    duplicateNames
            );
            throw new IllegalArgumentException(
                    "Der MacMahon-Export ist nicht eindeutig möglich, da folgende Namen im Turnier mehrfach vorkommen: "
                            + String.join(", ", duplicateNames)
                            + ". Bitte diese Teilnehmenden vor dem Export manuell unterscheiden."
            );
        }
    }

    /**
     * Builds a readable full name for display in user-facing validation messages.
     */
    private String buildReadableFullName(String firstName, String lastName) {
        String readableFirstName = firstName == null ? "" : firstName.trim();
        String readableLastName = lastName == null ? "" : lastName.trim();

        return (readableFirstName + " " + readableLastName)
                .trim()
                .replaceAll("\\s+", " ");
    }

    /**
     * Builds a normalized full name for case-insensitive duplicate detection.
     */
    private String normalizeFullName(String firstName, String lastName) {
        String normalizedFirstName = firstName == null ? "" : firstName.trim();
        String normalizedLastName = lastName == null ? "" : lastName.trim();

        return (normalizedFirstName + " " + normalizedLastName)
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}