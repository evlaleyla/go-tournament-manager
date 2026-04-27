package com.evlaleyla.gotournamentmanager.backend.participant;

import com.evlaleyla.gotournamentmanager.backend.ClubOptions;
import com.evlaleyla.gotournamentmanager.backend.CountryOptions;
import com.evlaleyla.gotournamentmanager.backend.RankOptions;
import com.evlaleyla.gotournamentmanager.backend.TournamentDataReferenceService;
import com.evlaleyla.gotournamentmanager.backend.registration.RegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Service layer for participant management.
 *
 * <p>This service is responsible for:
 * <ul>
 *     <li>loading and searching participants,</li>
 *     <li>normalizing and validating participant input,</li>
 *     <li>enforcing business rules for updates and deletions,</li>
 *     <li>preventing changes when participant names are already referenced
 *         in imported pairing or standing data.</li>
 * </ul>
 */
@Service
@Transactional
public class ParticipantService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantService.class);

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

    /**
     * Returns all participants.
     *
     * @return all persisted participants
     */
    @Transactional(readOnly = true)
    public List<Participant> findAll() {
        LOGGER.debug("Loading all participants.");
        return participantRepository.findAll();
    }

    /**
     * Searches participants using optional filter criteria.
     *
     * <p>Search values are normalized before being passed to the repository.
     *
     * @param firstName optional first-name filter
     * @param lastName optional last-name filter
     * @param country optional country filter
     * @param club optional club filter
     * @param rank optional rank filter
     * @return list of matching participants
     */
    @Transactional(readOnly = true)
    public List<Participant> search(String firstName,
                                    String lastName,
                                    String country,
                                    String club,
                                    String rank) {

        final String normalizedFirstName = normalizeSearch(firstName);
        final String normalizedLastName = normalizeSearch(lastName);
        final String normalizedCountry = normalizeOptionalCountry(country);
        final String normalizedClub = normalizeOptionalClub(club);
        final String normalizedRank = normalizeOptionalRank(rank);

        LOGGER.debug(
                "Searching participants with filters: firstName='{}', lastName='{}', country='{}', club='{}', rank='{}'.",
                normalizedFirstName,
                normalizedLastName,
                normalizedCountry,
                normalizedClub,
                normalizedRank
        );

        return participantRepository.search(
                normalizedFirstName,
                normalizedLastName,
                normalizedCountry,
                normalizedClub,
                normalizedRank
        );
    }

    /**
     * Creates and persists a new participant.
     *
     * <p>Before persistence, the service normalizes and validates relevant fields
     * such as e-mail address, country, club and rank.
     *
     * @param participant the participant to create
     * @return the saved participant
     * @throws IllegalArgumentException if validation fails or the e-mail is already in use
     */
    public Participant save(Participant participant) {
        LOGGER.debug("Creating a new participant.");

        final String normalizedEmail = normalizeEmail(participant.getEmail());
        final String normalizedCountry = CountryOptions.normalize(participant.getCountry());
        final String normalizedRank = RankOptions.normalize(participant.getRank());
        final String normalizedClub = ClubOptions.normalize(participant.getClub());

        if (participantRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            LOGGER.warn("Participant creation rejected because the e-mail address is already in use.");
            throw new IllegalArgumentException("Diese E-Mail-Adresse ist bereits einem anderen Teilnehmer zugeordnet.");
        }

        validateClubMatchesCountry(normalizedCountry, normalizedClub);

        applyNormalizedParticipantData(
                participant,
                normalizedEmail,
                normalizedCountry,
                normalizedRank,
                normalizedClub
        );

        Participant savedParticipant = participantRepository.save(participant);
        LOGGER.info("Participant created successfully with id={}.", savedParticipant.getId());

        return savedParticipant;
    }

    /**
     * Loads a participant by ID.
     *
     * @param id the participant ID
     * @return the matching participant
     * @throws IllegalArgumentException if no participant with the given ID exists
     */
    @Transactional(readOnly = true)
    public Participant findById(Long id) {
        LOGGER.debug("Loading participant with id={}.", id);

        return participantRepository.findById(id)
                .orElseThrow(() -> {
                    LOGGER.warn("Participant not found for id={}.", id);
                    return new IllegalArgumentException("Teilnehmer nicht gefunden: " + id);
                });
    }

    /**
     * Updates an existing participant.
     *
     * <p>Name changes are blocked if the participant name is already referenced
     * in imported pairing or standing data. E-mail uniqueness and club-country
     * consistency are validated as well.
     *
     * @param id the ID of the participant to update
     * @param updatedParticipant the new participant data
     * @return the updated and persisted participant
     * @throws IllegalArgumentException if business validation fails
     */
    public Participant update(Long id, Participant updatedParticipant) {
        LOGGER.debug("Updating participant with id={}.", id);

        Participant existingParticipant = findById(id);

        boolean firstNameChanged = !sameText(
                existingParticipant.getFirstName(),
                updatedParticipant.getFirstName()
        );

        boolean lastNameChanged = !sameText(
                existingParticipant.getLastName(),
                updatedParticipant.getLastName()
        );

        if ((firstNameChanged || lastNameChanged) && isNameLocked(existingParticipant)) {
            LOGGER.warn("Participant update rejected because the name is locked for id={}.", id);
            throw new IllegalArgumentException(
                    "Vorname und Nachname können nicht mehr geändert werden, " +
                            "da diese Person bereits in importierten Paarungen oder Ranglistendaten verwendet wird."
            );
        }

        final String normalizedEmail = normalizeEmail(updatedParticipant.getEmail());
        final String normalizedCountry = CountryOptions.normalize(updatedParticipant.getCountry());
        final String normalizedRank = RankOptions.normalize(updatedParticipant.getRank());
        final String normalizedClub = ClubOptions.normalize(updatedParticipant.getClub());

        if (participantRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
            LOGGER.warn("Participant update rejected because the e-mail address is already in use for id={}.", id);
            throw new IllegalArgumentException("Diese E-Mail-Adresse ist bereits einem anderen Teilnehmer zugeordnet.");
        }

        validateClubMatchesCountry(normalizedCountry, normalizedClub);

        existingParticipant.setFirstName(updatedParticipant.getFirstName());
        existingParticipant.setLastName(updatedParticipant.getLastName());
        existingParticipant.setBirthDate(updatedParticipant.getBirthDate());

        applyNormalizedParticipantData(
                existingParticipant,
                normalizedEmail,
                normalizedCountry,
                normalizedRank,
                normalizedClub
        );

        Participant savedParticipant = participantRepository.save(existingParticipant);
        LOGGER.info("Participant updated successfully with id={}.", savedParticipant.getId());

        return savedParticipant;
    }

    /**
     * Checks whether the participant name is locked due to external references.
     *
     * @param participantId the participant ID
     * @return {@code true} if the name must not be changed, otherwise {@code false}
     */
    @Transactional(readOnly = true)
    public boolean isNameLocked(Long participantId) {
        Participant participant = findById(participantId);
        return isNameLocked(participant);
    }

    /**
     * Deletes a participant if no blocking references exist.
     *
     * <p>A participant cannot be deleted if:
     * <ul>
     *     <li>registrations still exist for that participant,</li>
     *     <li>the participant name is referenced in imported pairings,</li>
     *     <li>the participant name is referenced in imported standings.</li>
     * </ul>
     *
     * @param id the ID of the participant to delete
     * @throws IllegalArgumentException if deletion is blocked by existing references
     */
    public void deleteById(Long id) {
        LOGGER.debug("Deleting participant with id={}.", id);

        Participant participant = findById(id);
        String participantName = participant.getFullName();

        if (registrationRepository.existsByParticipantId(id)) {
            LOGGER.warn("Participant deletion rejected because registrations still exist for id={}.", id);
            throw new IllegalArgumentException(
                    "Teilnehmer „" + participantName + "“ kann nicht gelöscht werden, da noch Anmeldungen für ihn existieren."
            );
        }

        if (tournamentDataReferenceService.hasPairingReferenceForParticipantName(participantName)) {
            LOGGER.warn("Participant deletion rejected because pairing references exist for id={}.", id);
            throw new IllegalArgumentException(
                    "Teilnehmer „" + participantName + "“ kann nicht gelöscht werden, " +
                            "da sein Name bereits in importierten Paarungen verwendet wird."
            );
        }

        if (tournamentDataReferenceService.hasStandingReferenceForParticipantName(participantName)) {
            LOGGER.warn("Participant deletion rejected because standing references exist for id={}.", id);
            throw new IllegalArgumentException(
                    "Teilnehmer „" + participantName + "“ kann nicht gelöscht werden, " +
                            "da sein Name bereits in importierten Ranglistendaten verwendet wird."
            );
        }

        participantRepository.deleteById(id);
        LOGGER.info("Participant deleted successfully with id={}.", id);
    }

    /**
     * Checks whether the participant name is referenced in imported tournament data.
     *
     * @param participant the participant to check
     * @return {@code true} if the name is already referenced externally, otherwise {@code false}
     */
    private boolean isNameLocked(Participant participant) {
        String participantName = participant.getFullName();

        return tournamentDataReferenceService.hasPairingReferenceForParticipantName(participantName)
                || tournamentDataReferenceService.hasStandingReferenceForParticipantName(participantName);
    }

    /**
     * Compares two text values after trimming and whitespace normalization.
     *
     * @param a first value
     * @param b second value
     * @return {@code true} if both values are textually equal after normalization
     */
    private boolean sameText(String a, String b) {
        String normalizedA = a == null ? "" : a.trim().replaceAll("\\s+", " ");
        String normalizedB = b == null ? "" : b.trim().replaceAll("\\s+", " ");
        return normalizedA.equals(normalizedB);
    }

    /**
     * Applies normalized persistence values to the participant entity.
     *
     * @param participant the participant entity to update
     * @param normalizedEmail normalized e-mail address
     * @param normalizedCountry normalized country code
     * @param normalizedRank normalized rank value
     * @param normalizedClub normalized club value
     */
    private void applyNormalizedParticipantData(Participant participant,
                                                String normalizedEmail,
                                                String normalizedCountry,
                                                String normalizedRank,
                                                String normalizedClub) {
        participant.setEmail(normalizedEmail);
        participant.setCountry(normalizedCountry);
        participant.setRank(normalizedRank);
        participant.setClub(normalizedClub);
    }

    /**
     * Normalizes an e-mail address for persistence and uniqueness checks.
     *
     * @param email the raw e-mail value
     * @return the normalized e-mail address or {@code null} if the input is {@code null}
     */
    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes a search input value.
     *
     * @param value the raw input
     * @return the trimmed value or {@code null} if the input is blank
     */
    private String normalizeSearch(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * Normalizes an optional country input.
     *
     * @param country the raw country value
     * @return the normalized country code or {@code null} if the input is blank
     */
    private String normalizeOptionalCountry(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }

        return CountryOptions.normalize(country);
    }

    /**
     * Normalizes an optional club input.
     *
     * @param club the raw club value
     * @return the normalized club value or {@code null} if the input is blank
     */
    private String normalizeOptionalClub(String club) {
        if (club == null || club.isBlank()) {
            return null;
        }

        return ClubOptions.normalize(club);
    }

    /**
     * Normalizes an optional rank input.
     *
     * @param rank the raw rank value
     * @return the normalized rank value or {@code null} if the input is blank
     */
    private String normalizeOptionalRank(String rank) {
        if (rank == null || rank.isBlank()) {
            return null;
        }

        return RankOptions.normalize(rank);
    }

    /**
     * Validates whether the given club is allowed for the selected country.
     *
     * @param country normalized country code
     * @param club normalized club value
     * @throws IllegalArgumentException if the club does not match the selected country
     */
    private void validateClubMatchesCountry(String country, String club) {
        if (!ClubOptions.isValidForCountry(country, club)) {
            LOGGER.warn("Club-country validation failed.");
            throw new IllegalArgumentException(
                    "Der Verein „" + club + "“ passt nicht zum ausgewählten Land."
            );
        }
    }
}