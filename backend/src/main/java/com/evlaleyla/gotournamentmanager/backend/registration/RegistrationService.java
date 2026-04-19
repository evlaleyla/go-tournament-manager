package com.evlaleyla.gotournamentmanager.backend.registration;

import com.evlaleyla.gotournamentmanager.backend.ClubOptions;
import com.evlaleyla.gotournamentmanager.backend.CountryOptions;
import com.evlaleyla.gotournamentmanager.backend.RankOptions;
import com.evlaleyla.gotournamentmanager.backend.participant.Participant;
import com.evlaleyla.gotournamentmanager.backend.participant.ParticipantRepository;
import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final TournamentRepository tournamentRepository;
    private final ParticipantRepository participantRepository;

    public RegistrationService(RegistrationRepository registrationRepository,
                               TournamentRepository tournamentRepository,
                               ParticipantRepository participantRepository) {
        this.registrationRepository = registrationRepository;
        this.tournamentRepository = tournamentRepository;
        this.participantRepository = participantRepository;
    }

    public List<Registration> findAll() {
        return registrationRepository.findAllByOrderByRegistrationDateDesc();
    }

    public List<Registration> findByTournamentId(Long tournamentId) {
        return registrationRepository.findByTournamentIdOrderByRegistrationDateAsc(tournamentId);
    }

    public List<Registration> findStartListByTournamentId(Long tournamentId) {
        return registrationRepository.findByTournamentIdOrderByParticipantLastNameAscParticipantFirstNameAsc(tournamentId);
    }
    public Registration findById(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + id));
    }

    public boolean existsByTournamentIdAndParticipantId(Long tournamentId, Long participantId) {
        if (tournamentId == null || participantId == null) {
            return false;
        }
        return registrationRepository.existsByTournamentIdAndParticipantId(tournamentId, participantId);
    }

    public boolean existsByTournamentIdAndParticipantEmail(Long tournamentId, String email) {
        if (tournamentId == null || email == null || email.isBlank()) {
            return false;
        }

        return participantRepository.findByEmailIgnoreCase(email.trim())
                .map(participant -> registrationRepository.existsByTournamentIdAndParticipantId(tournamentId, participant.getId()))
                .orElse(false);
    }

    public Registration create(RegistrationForm registrationForm) {
        Tournament tournament = tournamentRepository.findById(registrationForm.getTournamentId())
                .orElseThrow(() -> new IllegalArgumentException("Turnier nicht gefunden: " + registrationForm.getTournamentId()));

        Participant participant = participantRepository.findById(registrationForm.getParticipantId())
                .orElseThrow(() -> new IllegalArgumentException("Teilnehmer nicht gefunden: " + registrationForm.getParticipantId()));

        String normalizedCountry = CountryOptions.normalize(participant.getCountry());
        String normalizedRank = RankOptions.normalize(participant.getRank());
        String normalizedClub = ClubOptions.normalize(participant.getClub());

        validateClubMatchesCountry(normalizedCountry, normalizedClub);

        Registration registration = new Registration(
                tournament,
                participant,
                LocalDate.now(),
                registrationForm.getPlannedRounds(),
                normalizedRank,
                normalizedClub,
                normalizedCountry,
                registrationForm.getNotes()
        );
        return registrationRepository.save(registration);
    }
    public Registration createPublicRegistration(Long tournamentId, SelfRegistrationForm form) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Turnier nicht gefunden: " + tournamentId));

        String normalizedEmail = form.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        String normalizedCountry = CountryOptions.normalize(form.getCountry());
        String normalizedRank = RankOptions.normalize(form.getRank());
        String normalizedClub = ClubOptions.normalize(form.getClub());


        validateClubMatchesCountry(normalizedCountry, normalizedClub);

        Participant participant = participantRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> {
                    Participant newParticipant = new Participant();
                    newParticipant.setFirstName(form.getFirstName());
                    newParticipant.setLastName(form.getLastName());
                    newParticipant.setEmail(normalizedEmail);
                    newParticipant.setClub(normalizedClub);
                    newParticipant.setCountry(normalizedCountry);
                    newParticipant.setRank(normalizedRank);
                    newParticipant.setBirthDate(form.getBirthDate());
                    return participantRepository.save(newParticipant);
                });

        Registration registration = new Registration(
                tournament,
                participant,
                LocalDate.now(),
                form.getPlannedRounds(),
                normalizedRank,
                normalizedClub,
                normalizedCountry,
                form.getNotes()
        );

        return registrationRepository.save(registration);
    }
    public void deleteById(Long id) {
        registrationRepository.deleteById(id);
    }

    public List<Registration> findStartListByTournamentIdAndRound(Long tournamentId, int roundNumber) {
        return registrationRepository
                .findByTournamentIdOrderByParticipantLastNameAscParticipantFirstNameAsc(tournamentId)
                .stream()
                .filter(registration -> registration.getPlannedRounds() != null
                        && registration.getPlannedRounds() >= roundNumber)
                .collect(Collectors.toList());
    }

    private void validateClubMatchesCountry(String country, String club) {
        if (!ClubOptions.isValidForCountry(country, club)) {
            throw new IllegalArgumentException(
                    "Der Verein „" + club + "“ passt nicht zum ausgewählten Land."
            );
        }
    }
}