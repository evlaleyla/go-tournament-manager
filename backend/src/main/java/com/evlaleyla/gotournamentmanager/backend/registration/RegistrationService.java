package com.evlaleyla.gotournamentmanager.backend.registration;

import com.evlaleyla.gotournamentmanager.backend.ClubOptions;
import com.evlaleyla.gotournamentmanager.backend.CountryOptions;
import com.evlaleyla.gotournamentmanager.backend.RankOptions;
import com.evlaleyla.gotournamentmanager.backend.TournamentDataReferenceService;
import com.evlaleyla.gotournamentmanager.backend.participant.Participant;
import com.evlaleyla.gotournamentmanager.backend.participant.ParticipantRepository;
import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import com.evlaleyla.gotournamentmanager.backend.tournament.TournamentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Service responsible for managing tournament registrations.
 *
 * <p>This service encapsulates the business logic for creating, updating,
 * retrieving, and deleting registrations. It also ensures that registration
 * data remains consistent with participant data and imported tournament data
 * such as pairings and standings.</p>
 */
@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    private final RegistrationRepository registrationRepository;
    private final TournamentRepository tournamentRepository;
    private final ParticipantRepository participantRepository;
    private final TournamentDataReferenceService tournamentDataReferenceService;

    public RegistrationService(RegistrationRepository registrationRepository,
                               TournamentRepository tournamentRepository,
                               ParticipantRepository participantRepository,
                               TournamentDataReferenceService tournamentDataReferenceService) {
        this.registrationRepository = registrationRepository;
        this.tournamentRepository = tournamentRepository;
        this.participantRepository = participantRepository;
        this.tournamentDataReferenceService = tournamentDataReferenceService;
    }

    /**
     * Returns all registrations ordered by registration date descending.
     *
     * @return all registrations
     */
    @Transactional(readOnly = true)
    public List<Registration> findAll() {
        logger.debug("Loading all registrations ordered by registration date descending.");
        return registrationRepository.findAllByOrderByRegistrationDateDesc();
    }

    /**
     * Returns all registrations for a specific tournament ordered by registration date ascending.
     *
     * @param tournamentId the tournament id
     * @return registrations of the given tournament
     */
    @Transactional(readOnly = true)
    public List<Registration> findByTournamentId(Long tournamentId) {
        logger.debug("Loading registrations for tournamentId={}.", tournamentId);
        return registrationRepository.findByTournamentIdOrderByRegistrationDateAsc(tournamentId);
    }

    /**
     * Returns the start list of a tournament ordered by participant name.
     *
     * @param tournamentId the tournament id
     * @return registrations for the tournament start list
     */
    @Transactional(readOnly = true)
    public List<Registration> findStartListByTournamentId(Long tournamentId) {
        logger.debug("Loading start list for tournamentId={}.", tournamentId);
        return registrationRepository.findByTournamentIdOrderByParticipantLastNameAscParticipantFirstNameAsc(
                tournamentId
        );
    }

    /**
     * Finds a registration by id.
     *
     * @param id the registration id
     * @return the matching registration
     * @throws IllegalArgumentException if the registration does not exist
     */
    @Transactional(readOnly = true)
    public Registration findById(Long id) {
        logger.debug("Loading registration with id={}.", id);

        return registrationRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Registration with id={} was not found.", id);
                    return new IllegalArgumentException("Anmeldung nicht gefunden: " + id);
                });
    }

    /**
     * Checks whether a registration already exists for the given tournament and participant.
     *
     * @param tournamentId the tournament id
     * @param participantId the participant id
     * @return {@code true} if a registration exists, otherwise {@code false}
     */
    @Transactional(readOnly = true)
    public boolean existsByTournamentIdAndParticipantId(Long tournamentId, Long participantId) {
        if (tournamentId == null || participantId == null) {
            logger.debug("Skipping registration existence check because tournamentId or participantId is null.");
            return false;
        }

        boolean exists = registrationRepository.existsByTournamentIdAndParticipantId(tournamentId, participantId);
        logger.debug(
                "Checked registration existence for tournamentId={} and participantId={}: {}",
                tournamentId,
                participantId,
                exists
        );
        return exists;
    }

    /**
     * Checks whether a registration already exists for the given tournament and participant email.
     *
     * @param tournamentId the tournament id
     * @param email the participant email
     * @return {@code true} if a registration exists, otherwise {@code false}
     */
    @Transactional(readOnly = true)
    public boolean existsByTournamentIdAndParticipantEmail(Long tournamentId, String email) {
        if (tournamentId == null || email == null || email.isBlank()) {
            logger.debug("Skipping registration existence check because tournamentId or email is missing.");
            return false;
        }

        String normalizedEmail = normalizeEmail(email);

        boolean exists = participantRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(participant ->
                        registrationRepository.existsByTournamentIdAndParticipantId(tournamentId, participant.getId()))
                .orElse(false);

        logger.debug(
                "Checked registration existence for tournamentId={} and participantEmail={}: {}",
                tournamentId,
                normalizedEmail,
                exists
        );

        return exists;
    }

    /**
     * Creates an internal registration for an existing participant and tournament.
     *
     * @param registrationForm the submitted registration form
     * @return the persisted registration
     */
    @Transactional
    public Registration create(RegistrationForm registrationForm) {
        logger.info(
                "Creating internal registration for tournamentId={} and participantId={}.",
                registrationForm.getTournamentId(),
                registrationForm.getParticipantId()
        );

        Tournament tournament = loadTournament(registrationForm.getTournamentId());
        Participant participant = loadParticipant(registrationForm.getParticipantId());

        String normalizedCountry = CountryOptions.normalize(participant.getCountry());
        String normalizedRank = RankOptions.normalize(participant.getRank());
        String normalizedClub = ClubOptions.normalize(participant.getClub());

        validateClubMatchesCountry(normalizedCountry, normalizedClub);

        Registration registration = new Registration(
                tournament,
                participant,
                LocalDate.now(),
                normalizeSelectedRounds(registrationForm.getSelectedRounds()),
                normalizedRank,
                normalizedClub,
                normalizedCountry,
                registrationForm.getNotes()
        );

        Registration savedRegistration = registrationRepository.save(registration);

        logger.info(
                "Created internal registration with id={} for tournamentId={} and participantId={}.",
                savedRegistration.getId(),
                tournament.getId(),
                participant.getId()
        );

        return savedRegistration;
    }

    /**
     * Creates a public self-registration.
     *
     * <p>If a participant with the given email already exists, that participant is reused.
     * Otherwise, a new participant is created first.</p>
     *
     * @param tournamentId the tournament id
     * @param form the public self-registration form
     * @return the persisted registration
     */
    @Transactional
    public Registration createPublicRegistration(Long tournamentId, SelfRegistrationForm form) {
        logger.info("Creating public registration for tournamentId={} and email={}.", tournamentId, form.getEmail());

        Tournament tournament = loadTournament(tournamentId);

        String normalizedEmail = normalizeEmail(form.getEmail());
        String normalizedCountry = CountryOptions.normalize(form.getCountry());
        String normalizedRank = RankOptions.normalize(form.getRank());
        String normalizedClub = ClubOptions.normalize(form.getClub());

        validateClubMatchesCountry(normalizedCountry, normalizedClub);

        Participant participant = participantRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> createParticipantFromPublicForm(
                        form,
                        normalizedEmail,
                        normalizedCountry,
                        normalizedRank,
                        normalizedClub
                ));

        Registration registration = new Registration(
                tournament,
                participant,
                LocalDate.now(),
                normalizeSelectedRounds(form.getSelectedRounds()),
                normalizedRank,
                normalizedClub,
                normalizedCountry,
                form.getNotes()
        );

        Registration savedRegistration = registrationRepository.save(registration);

        logger.info(
                "Created public registration with id={} for tournamentId={} and participantId={}.",
                savedRegistration.getId(),
                tournament.getId(),
                participant.getId()
        );

        return savedRegistration;
    }

    /**
     * Deletes a registration if no imported pairings or standing data reference it.
     *
     * @param id the registration id
     */
    @Transactional
    public void deleteById(Long id) {
        logger.info("Deleting registration with id={}.", id);

        Registration registration = findById(id);
        Long tournamentId = registration.getTournament().getId();
        String participantName = registration.getParticipant().getFullName();

        if (tournamentDataReferenceService.hasPairingReferenceForTournamentAndParticipantName(
                tournamentId,
                participantName
        )) {
            logger.warn(
                    "Deletion blocked for registrationId={} because imported pairings reference participant='{}' in tournamentId={}.",
                    id,
                    participantName,
                    tournamentId
            );
            throw new IllegalArgumentException(
                    "Die Anmeldung von „" + participantName + "“ kann nicht gelöscht werden, " +
                            "da für dieses Turnier bereits Paarungen mit dieser Person vorliegen."
            );
        }

        if (tournamentDataReferenceService.hasStandingReferenceForTournamentAndParticipantName(
                tournamentId,
                participantName
        )) {
            logger.warn(
                    "Deletion blocked for registrationId={} because imported standing data reference participant='{}' in tournamentId={}.",
                    id,
                    participantName,
                    tournamentId
            );
            throw new IllegalArgumentException(
                    "Die Anmeldung von „" + participantName + "“ kann nicht gelöscht werden, " +
                            "da für dieses Turnier bereits Ranglistendaten mit dieser Person vorliegen."
            );
        }

        registrationRepository.deleteById(id);
        logger.info("Deleted registration with id={}.", id);
    }

    /**
     * Returns the start list entries of a tournament that are eligible for a given round.
     *
     * @param tournamentId the tournament id
     * @param roundNumber the round number
     * @return registrations eligible for the given round
     */
    @Transactional(readOnly = true)
    public List<Registration> findStartListByTournamentIdAndRound(Long tournamentId, int roundNumber) {
        logger.debug("Loading round-specific start list for tournamentId={} and roundNumber={}.", tournamentId, roundNumber);

        return registrationRepository
                .findByTournamentIdOrderByParticipantLastNameAscParticipantFirstNameAsc(tournamentId)
                .stream()
                .filter(registration -> registration.isPlayingInRound(roundNumber))
                .collect(Collectors.toList());
    }

    /**
     * Checks whether a registration already exists for the given tournament and participant,
     * excluding the current registration id.
     *
     * @param tournamentId the tournament id
     * @param participantId the participant id
     * @param registrationId the current registration id to exclude
     * @return {@code true} if another registration exists, otherwise {@code false}
     */
    @Transactional(readOnly = true)
    public boolean existsByTournamentIdAndParticipantIdAndIdNot(Long tournamentId,
                                                                Long participantId,
                                                                Long registrationId) {
        if (tournamentId == null || participantId == null || registrationId == null) {
            logger.debug("Skipping duplicate registration check because at least one argument is null.");
            return false;
        }

        boolean exists = registrationRepository.existsByTournamentIdAndParticipantIdAndIdNot(
                tournamentId,
                participantId,
                registrationId
        );

        logger.debug(
                "Checked duplicate registration for tournamentId={}, participantId={}, excluding registrationId={}: {}",
                tournamentId,
                participantId,
                registrationId,
                exists
        );

        return exists;
    }

    /**
     * Updates an existing registration.
     *
     * <p>If imported pairings or standings already reference this registration,
     * the participant and tournament assignment can no longer be changed.</p>
     *
     * @param id the registration id
     * @param registrationForm the updated registration form
     * @return the updated registration
     */
    @Transactional
    public Registration update(Long id, RegistrationForm registrationForm) {
        logger.info("Updating registration with id={}.", id);

        Registration existingRegistration = findById(id);

        boolean participantChanged = !existingRegistration.getParticipant().getId()
                .equals(registrationForm.getParticipantId());

        boolean tournamentChanged = !existingRegistration.getTournament().getId()
                .equals(registrationForm.getTournamentId());

        if ((participantChanged || tournamentChanged) && isParticipantOrTournamentLocked(existingRegistration)) {
            logger.warn(
                    "Update blocked for registrationId={} because participant or tournament is locked by imported data.",
                    id
            );
            throw new IllegalArgumentException(
                    "Teilnehmer und Turnier können nicht mehr geändert werden, " +
                            "da diese Anmeldung bereits in importierten Paarungen oder Ranglistendaten verwendet wird. " +
                            "Es können nur noch ausgewählte Runden und Notizen geändert werden."
            );
        }

        Tournament tournament = loadTournament(registrationForm.getTournamentId());
        Participant participant = loadParticipant(registrationForm.getParticipantId());

        String normalizedCountry = CountryOptions.normalize(participant.getCountry());
        String normalizedRank = RankOptions.normalize(participant.getRank());
        String normalizedClub = ClubOptions.normalize(participant.getClub());

        validateClubMatchesCountry(normalizedCountry, normalizedClub);

        existingRegistration.setTournament(tournament);
        existingRegistration.setParticipant(participant);
        existingRegistration.setSelectedRounds(normalizeSelectedRounds(registrationForm.getSelectedRounds()));
        existingRegistration.setRankAtRegistration(normalizedRank);
        existingRegistration.setClubAtRegistration(normalizedClub);
        existingRegistration.setCountryAtRegistration(normalizedCountry);
        existingRegistration.setNotes(registrationForm.getNotes());

        Registration updatedRegistration = registrationRepository.save(existingRegistration);

        logger.info(
                "Updated registration with id={} for tournamentId={} and participantId={}.",
                updatedRegistration.getId(),
                tournament.getId(),
                participant.getId()
        );

        return updatedRegistration;
    }

    /**
     * Checks whether the participant or tournament assignment of a registration is locked.
     *
     * @param registrationId the registration id
     * @return {@code true} if locked, otherwise {@code false}
     */
    @Transactional(readOnly = true)
    public boolean isParticipantOrTournamentLocked(Long registrationId) {
        Registration registration = findById(registrationId);
        return isParticipantOrTournamentLocked(registration);
    }

    /**
     * Determines whether the participant or tournament assignment is locked because
     * imported pairings or standing data already reference the registration.
     *
     * @param registration the registration to inspect
     * @return {@code true} if the assignment is locked, otherwise {@code false}
     */
    private boolean isParticipantOrTournamentLocked(Registration registration) {
        Long tournamentId = registration.getTournament().getId();
        String participantName = registration.getParticipant().getFullName();

        return tournamentDataReferenceService.hasPairingReferenceForTournamentAndParticipantName(
                tournamentId,
                participantName
        ) || tournamentDataReferenceService.hasStandingReferenceForTournamentAndParticipantName(
                tournamentId,
                participantName
        );
    }

    /**
     * Loads a tournament and throws a domain-specific exception if it does not exist.
     *
     * @param tournamentId the tournament id
     * @return the loaded tournament
     */
    private Tournament loadTournament(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> {
                    logger.warn("Tournament with id={} was not found.", tournamentId);
                    return new IllegalArgumentException("Turnier nicht gefunden: " + tournamentId);
                });
    }

    /**
     * Loads a participant and throws a domain-specific exception if it does not exist.
     *
     * @param participantId the participant id
     * @return the loaded participant
     */
    private Participant loadParticipant(Long participantId) {
        return participantRepository.findById(participantId)
                .orElseThrow(() -> {
                    logger.warn("Participant with id={} was not found.", participantId);
                    return new IllegalArgumentException("Teilnehmer nicht gefunden: " + participantId);
                });
    }

    /**
     * Creates and persists a new participant from the public registration form.
     *
     * @param form the public registration form
     * @param normalizedEmail normalized email value
     * @param normalizedCountry normalized country value
     * @param normalizedRank normalized rank value
     * @param normalizedClub normalized club value
     * @return the newly created participant
     */
    private Participant createParticipantFromPublicForm(SelfRegistrationForm form,
                                                        String normalizedEmail,
                                                        String normalizedCountry,
                                                        String normalizedRank,
                                                        String normalizedClub) {
        logger.info("Creating new participant from public registration for email={}.", normalizedEmail);

        Participant newParticipant = new Participant();
        newParticipant.setFirstName(form.getFirstName());
        newParticipant.setLastName(form.getLastName());
        newParticipant.setEmail(normalizedEmail);
        newParticipant.setClub(normalizedClub);
        newParticipant.setCountry(normalizedCountry);
        newParticipant.setRank(normalizedRank);
        newParticipant.setBirthDate(form.getBirthDate());

        Participant savedParticipant = participantRepository.save(newParticipant);

        logger.info("Created new participant with id={} for email={}.", savedParticipant.getId(), normalizedEmail);

        return savedParticipant;
    }

    /**
     * Normalizes the selected rounds into a stable, sorted set.
     *
     * @param selectedRounds selected round numbers
     * @return sorted set of unique round numbers
     */
    private Set<Integer> normalizeSelectedRounds(List<Integer> selectedRounds) {
        if (selectedRounds == null || selectedRounds.isEmpty()) {
            logger.warn("Selected rounds validation failed because no rounds were provided.");
            throw new IllegalArgumentException("Bitte mindestens eine Runde auswählen.");
        }

        return new LinkedHashSet<>(new TreeSet<>(selectedRounds));
    }

    /**
     * Validates whether the selected club is compatible with the selected country.
     *
     * @param country normalized country code
     * @param club normalized club name
     */
    private void validateClubMatchesCountry(String country, String club) {
        if (!ClubOptions.isValidForCountry(country, club)) {
            logger.warn("Club-country validation failed for club='{}' and country='{}'.", club, country);
            throw new IllegalArgumentException(
                    "Der Verein „" + club + "“ passt nicht zum ausgewählten Land."
            );
        }
    }

    /**
     * Normalizes an email address for consistent lookups and persistence.
     *
     * @param email the raw email value
     * @return normalized email in lower case
     */
    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}