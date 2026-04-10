package com.evlaleyla.gotournamentmanager.backend.registration;

import com.evlaleyla.gotournamentmanager.backend.participant.Participant;
import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_id", "participant_id"})
)
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registrationDate;

    @Column(length = 1000)
    private String notes;

    public Registration() {
    }

    public Registration(Tournament tournament, Participant participant, LocalDate registrationDate, String notes) {
        this.tournament = tournament;
        this.participant = participant;
        this.registrationDate = registrationDate;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public Participant getParticipant() {
        return participant;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public void setParticipant(Participant participant) {
        this.participant = participant;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}