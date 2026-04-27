package com.evlaleyla.gotournamentmanager.backend.participant;

import com.evlaleyla.gotournamentmanager.backend.ClubOptions;
import com.evlaleyla.gotournamentmanager.backend.CountryOptions;
import com.evlaleyla.gotournamentmanager.backend.RankOptions;
import com.evlaleyla.gotournamentmanager.backend.TournamentDataReferenceService;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final RegistrationRepository registrationRepository;
    private final TournamentDataReferenceService tournamentDataReferenceService;

    public ParticipantService(ParticipantRepository participantRepository,
                              RegistrationRepository registrationRepository,
                              TournamentDataReferenceService tournamentDataReferenceService) {
        this.participantRepository = participantRepository;
        this.registrationRepository = registrationRepository;
        this.tournamentDataReferenceService = tournamentDataReferenceService;
    }

    public List<Participant> findAll() {
        return participantRepository.findAll();
    }

    public List<Participant> search(String firstName,
                                    String lastName,
                                    String country,
                                    String club,
                                    String rank) {

        String normalizedFirstName = normalizeSearch(firstName);
        String normalizedLastName = normalizeSearch(lastName);
        String normalizedCountry = normalizeOptionalCountry(country);
        String normalizedClub = normalizeOptionalClub(club);
        String normalizedRank = normalizeOptionalRank(rank);

        return participantRepository.search(
                normalizedFirstName,
                normalizedLastName,
                normalizedCountry,
                normalizedClub,
                normalizedRank
        );
    }

    public Participant save(Participant participant) {
        String normalizedEmail = normalizeEmail(participant.getEmail());
        String normalizedCountry = CountryOptions.normalize(participant.getCountry());
        String normalizedRank = RankOptions.normalize(participant.getRank());
        String normalizedClub = ClubOptions.normalize(participant.getClub());

        if (participantRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Diese E-Mail-Adresse ist bereits einem anderen Teilnehmer zugeordnet.");
        }

        validateClubMatchesCountry(normalizedCountry, normalizedClub);

        participant.setEmail(normalizedEmail);
        participant.setCountry(normalizedCountry);
        participant.setRank(normalizedRank);
        participant.setClub(normalizedClub);

        return participantRepository.save(participant);
    }

    public Participant findById(Long id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teilnehmer nicht gefunden: " + id));
    }

    public Participant update(Long id, Participant updatedParticipant) {
        Participant existingParticipant = findById(id);

        String normalizedEmail = normalizeEmail(updatedParticipant.getEmail());
        String normalizedCountry = CountryOptions.normalize(updatedParticipant.getCountry());
        String normalizedRank = RankOptions.normalize(updatedParticipant.getRank());
        String normalizedClub = ClubOptions.normalize(updatedParticipant.getClub());

        if (participantRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
            throw new IllegalArgumentException("Diese E-Mail-Adresse ist bereits einem anderen Teilnehmer zugeordnet.");
        }

        validateClubMatchesCountry(normalizedCountry, normalizedClub);

        existingParticipant.setFirstName(updatedParticipant.getFirstName());
        existingParticipant.setLastName(updatedParticipant.getLastName());
        existingParticipant.setEmail(normalizedEmail);
        existingParticipant.setClub(normalizedClub);
        existingParticipant.setCountry(normalizedCountry);
        existingParticipant.setRank(normalizedRank);
        existingParticipant.setBirthDate(updatedParticipant.getBirthDate());

        return participantRepository.save(existingParticipant);
    }

    public void deleteById(Long id) {
        Participant participant = findById(id);
        String participantName = participant.getFullName();

        if (registrationRepository.existsByParticipantId(id)) {
            throw new IllegalArgumentException(
                    "Teilnehmer „" + participantName + "“ kann nicht gelöscht werden, da noch Anmeldungen für ihn existieren."
            );
        }

        if (tournamentDataReferenceService.hasPairingReferenceForParticipantName(participantName)) {
            throw new IllegalArgumentException(
                    "Teilnehmer „" + participantName + "“ kann nicht gelöscht werden, " +
                            "da sein Name bereits in importierten Paarungen verwendet wird."
            );
        }

        if (tournamentDataReferenceService.hasStandingReferenceForParticipantName(participantName)) {
            throw new IllegalArgumentException(
                    "Teilnehmer „" + participantName + "“ kann nicht gelöscht werden, " +
                            "da sein Name bereits in importierten Ranglistendaten verwendet wird."
            );
        }

        participantRepository.deleteById(id);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeSearch(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeOptionalCountry(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }
        return CountryOptions.normalize(country);
    }

    private String normalizeOptionalClub(String club) {
        if (club == null || club.isBlank()) {
            return null;
        }
        return ClubOptions.normalize(club);
    }

    private String normalizeOptionalRank(String rank) {
        if (rank == null || rank.isBlank()) {
            return null;
        }
        return RankOptions.normalize(rank);
    }

    private void validateClubMatchesCountry(String country, String club) {
        if (!ClubOptions.isValidForCountry(country, club)) {
            throw new IllegalArgumentException(
                    "Der Verein „" + club + "“ passt nicht zum ausgewählten Land."
            );
        }
    }
}