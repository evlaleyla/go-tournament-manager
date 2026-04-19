package com.evlaleyla.gotournamentmanager.backend.participant;

import com.evlaleyla.gotournamentmanager.backend.ClubOptions;
import com.evlaleyla.gotournamentmanager.backend.CountryOptions;
import com.evlaleyla.gotournamentmanager.backend.RankOptions;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final RegistrationRepository registrationRepository;

    public ParticipantService(ParticipantRepository participantRepository,
                              RegistrationRepository registrationRepository) {
        this.participantRepository = participantRepository;
        this.registrationRepository = registrationRepository;
    }

    public List<Participant> findAll() {
        return participantRepository.findAll();
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

        if (registrationRepository.existsByParticipantId(id)) {
            throw new IllegalArgumentException(
                    "Teilnehmer „" + participant.getFullName() + "“ kann nicht gelöscht werden, da noch Anmeldungen für ihn existieren."
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

    private void validateClubMatchesCountry(String country, String club) {
        if (!ClubOptions.isValidForCountry(country, club)) {
            throw new IllegalArgumentException(
                    "Der Verein „" + club + "“ passt nicht zum ausgewählten Land."
            );
        }
    }
}