package com.evlaleyla.gotournamentmanager.backend.tournament;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for accessing and querying {@link Tournament} entities.
 *
 * <p>This repository provides search methods used by the administrative
 * tournament overview and related UI components.</p>
 */
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    /**
     * Finds all tournaments whose name contains the given search term,
     * ignoring letter case.
     *
     * @param name the search term to match against the tournament name
     * @return all matching tournaments
     */
    List<Tournament> findByNameContainingIgnoreCase(String name);

    /**
     * Finds all tournaments with the given status.
     *
     * @param status the tournament status to filter by
     * @return all tournaments with the given status
     */
    List<Tournament> findByStatus(TournamentStatus status);

    /**
     * Finds all tournaments whose name contains the given search term
     * and that match the given status.
     *
     * @param name the search term to match against the tournament name
     * @param status the tournament status to filter by
     * @return all tournaments matching both criteria
     */
    List<Tournament> findByNameContainingIgnoreCaseAndStatus(String name, TournamentStatus status);

    /**
     * Returns all distinct tournament names in ascending alphabetical order.
     *
     * <p>This method is used, for example, to populate suggestion lists
     * for name-based search fields.</p>
     *
     * @return sorted list of distinct tournament names
     */
    @Query("select distinct t.name from Tournament t order by t.name asc")
    List<String> findDistinctNames();

    /**
     * Searches tournaments using optional filter criteria.
     *
     * <p>Each parameter is optional. If a parameter is {@code null} or empty,
     * it is ignored in the query.</p>
     *
     * @param search optional name search term
     * @param status optional tournament status filter
     * @param location optional location filter
     * @param startDateFrom optional lower bound for the start date
     * @param startDateTo optional upper bound for the start date
     * @return list of matching tournaments ordered by start date and name
     */
    @Query("""
            select t
            from Tournament t
            where
                (
                    :search is null
                    or :search = ''
                    or lower(t.name) like lower(concat('%', :search, '%'))
                )
            and
                (
                    :status is null
                    or t.status = :status
                )
            and
                (
                    :location is null
                    or :location = ''
                    or lower(t.location) like lower(concat('%', :location, '%'))
                )
            and
                (
                    :startDateFrom is null
                    or t.startDate >= :startDateFrom
                )
            and
                (
                    :startDateTo is null
                    or t.startDate <= :startDateTo
                )
            order by t.startDate asc, t.name asc
            """)
    List<Tournament> search(
            @Param("search") String search,
            @Param("status") TournamentStatus status,
            @Param("location") String location,
            @Param("startDateFrom") LocalDate startDateFrom,
            @Param("startDateTo") LocalDate startDateTo
    );
}