package com.evlaleyla.gotournamentmanager.backend.participant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing and querying {@link Participant} entities.
 *
 * <p>This repository provides:
 * <ul>
 *     <li>lookup by e-mail address,</li>
 *     <li>existence checks for uniqueness validation,</li>
 *     <li>a flexible search query with optional filter parameters.</li>
 * </ul>
 */
public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    /**
     * Finds a participant by e-mail address using a case-insensitive comparison.
     *
     * @param email the e-mail address to search for
     * @return an {@link Optional} containing the matching participant if found
     */
    Optional<Participant> findByEmailIgnoreCase(String email);

    /**
     * Checks whether a participant with the given e-mail address already exists.
     *
     * @param email the e-mail address to check
     * @return {@code true} if a participant with that e-mail exists, otherwise {@code false}
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Checks whether a participant with the given e-mail address exists,
     * excluding a specific participant ID.
     *
     * <p>This is typically used during update operations to ensure e-mail uniqueness
     * without treating the current entity as a conflict.
     *
     * @param email the e-mail address to check
     * @param id the participant ID to exclude from the check
     * @return {@code true} if another participant with that e-mail exists, otherwise {@code false}
     */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /**
     * Searches participants using optional filter criteria.
     *
     * <p>Each parameter is optional. If a parameter is {@code null} or empty,
     * it is ignored in the query.
     *
     * <p>Filtering behavior:
     * <ul>
     *     <li>{@code firstName}: case-insensitive partial match</li>
     *     <li>{@code lastName}: case-insensitive partial match</li>
     *     <li>{@code country}: case-insensitive exact match</li>
     *     <li>{@code club}: case-insensitive exact match</li>
     *     <li>{@code rank}: case-insensitive exact match</li>
     * </ul>
     *
     * <p>The result is ordered alphabetically by last name and first name.
     *
     * @param firstName optional first-name filter
     * @param lastName optional last-name filter
     * @param country optional country filter
     * @param club optional club filter
     * @param rank optional rank filter
     * @return a list of matching participants
     */
    @Query("""
            select p
            from Participant p
            where
                (
                    :firstName is null
                    or :firstName = ''
                    or lower(p.firstName) like lower(concat('%', :firstName, '%'))
                )
            and
                (
                    :lastName is null
                    or :lastName = ''
                    or lower(p.lastName) like lower(concat('%', :lastName, '%'))
                )
            and
                (
                    :country is null
                    or :country = ''
                    or lower(p.country) = lower(:country)
                )
            and
                (
                    :club is null
                    or :club = ''
                    or lower(p.club) = lower(:club)
                )
            and
                (
                    :rank is null
                    or :rank = ''
                    or lower(p.rank) = lower(:rank)
                )
            order by p.lastName asc, p.firstName asc
            """)
    List<Participant> search(
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("country") String country,
            @Param("club") String club,
            @Param("rank") String rank
    );
}