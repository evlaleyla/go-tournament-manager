package com.evlaleyla.gotournamentmanager.backend.registration;

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

        Registration registration = new Registration(
                tournament,
                participant,
                LocalDate.now(),
                registrationForm.getPlannedRounds(),
                participant.getRank(),
                participant.getClub(),
                participant.getCountry(),
                registrationForm.getNotes()
        );

        return registrationRepository.save(registration);
    }
    public Registration createPublicRegistration(Long tournamentId, SelfRegistrationForm form) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Turnier nicht gefunden: " + tournamentId));

        String normalizedEmail = form.getEmail().trim().toLowerCase(java.util.Locale.ROOT);

        Participant participant = participantRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> {
                    Participant newParticipant = new Participant();
                    newParticipant.setFirstName(form.getFirstName());
                    newParticipant.setLastName(form.getLastName());
                    newParticipant.setEmail(normalizedEmail);
                    newParticipant.setClub(form.getClub());
                    newParticipant.setCountry(form.getCountry());
                    newParticipant.setRank(form.getRank());
                    newParticipant.setBirthDate(form.getBirthDate());
                    return participantRepository.save(newParticipant);
                });

        Registration registration = new Registration(
                tournament,
                participant,
                LocalDate.now(),
                form.getPlannedRounds(),
                form.getRank(),
                form.getClub(),
                form.getCountry(),
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
}