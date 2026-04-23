package com.evlaleyla.gotournamentmanager.backend.participant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

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