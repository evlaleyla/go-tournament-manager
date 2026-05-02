package com.evlaleyla.gotournamentmanager.backend.registration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for accessing and managing {@link Registration} entities.
 *
 * <p>This repository provides query methods for retrieving registrations
 * by tournament, participant, and common existence checks required by
 * the service layer.</p>
 */
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    /**
     * Returns all registrations ordered by registration date descending.
     *
     * @return all registrations sorted by newest registration first
     */
    List<Registration> findAllByOrderByRegistrationDateDesc();

    /**
     * Returns all registrations of a tournament ordered by registration date ascending.
     *
     * @param tournamentId the tournament identifier
     * @return registrations of the given tournament sorted by registration date
     */
    List<Registration> findByTournamentIdOrderByRegistrationDateAsc(Long tournamentId);

    /**
     * Returns the start list of a tournament ordered by participant name.
     *
     * <p>This ordering is useful for exports, printed lists, and deterministic
     * processing of registrations within a tournament context.</p>
     *
     * @param tournamentId the tournament identifier
     * @return registrations sorted by participant last name and first name
     */
    List<Registration> findByTournamentIdOrderByParticipantLastNameAscParticipantFirstNameAsc(Long tournamentId);

    /**
     * Checks whether a registration already exists for the given tournament
     * and participant.
     *
     * @param tournamentId the tournament identifier
     * @param participantId the participant identifier
     * @return {@code true} if such a registration exists, otherwise {@code false}
     */
    boolean existsByTournamentIdAndParticipantId(Long tournamentId, Long participantId);

    /**
     * Checks whether at least one registration exists for the given participant.
     *
     * @param participantId the participant identifier
     * @return {@code true} if the participant has at least one registration
     */
    boolean existsByParticipantId(Long participantId);

    /**
     * Deletes all registrations belonging to the given tournament.
     *
     * @param tournamentId the tournament identifier
     */
    void deleteByTournamentId(Long tournamentId);

    /**
     * Checks whether a registration exists for the given tournament and participant,
     * excluding a specific registration id.
     *
     * <p>This is typically used during update operations to enforce uniqueness
     * without matching the entity currently being edited.</p>
     *
     * @param tournamentId the tournament identifier
     * @param participantId the participant identifier
     * @param id the registration id that should be excluded from the check
     * @return {@code true} if another matching registration exists
     */
    boolean existsByTournamentIdAndParticipantIdAndIdNot(Long tournamentId,
                                                         Long participantId,
                                                         Long id);

    /**
     * Counts the number of registrations assigned to a tournament.
     *
     * @param tournamentId the tournament identifier
     * @return number of registrations for the given tournament
     */
    long countByTournamentId(Long tournamentId);
}