package com.evlaleyla.gotournamentmanager.backend.registration;

import com.evlaleyla.gotournamentmanager.backend.participant.Participant;
import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Die geplante Rundenzahl ist erforderlich.")
    @Min(value = 1, message = "Die geplante Rundenzahl muss mindestens 1 sein.")
    private Integer plannedRounds;

    @NotBlank(message = "Der Rang ist erforderlich.")
    private String rankAtRegistration;

    private String clubAtRegistration;

    private String countryAtRegistration;

    @Column(length = 1000)
    private String notes;

    public Registration() {
    }

    public Registration(Tournament tournament,
                        Participant participant,
                        LocalDate registrationDate,
                        Integer plannedRounds,
                        String rankAtRegistration,
                        String clubAtRegistration,
                        String countryAtRegistration,
                        String notes) {
        this.tournament = tournament;
        this.participant = participant;
        this.registrationDate = registrationDate;
        this.plannedRounds = plannedRounds;
        this.rankAtRegistration = rankAtRegistration;
        this.clubAtRegistration = clubAtRegistration;
        this.countryAtRegistration = countryAtRegistration;
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

    public Integer getPlannedRounds() {
        return plannedRounds;
    }

    public String getRankAtRegistration() {
        return rankAtRegistration;
    }

    public String getClubAtRegistration() {
        return clubAtRegistration;
    }

    public String getCountryAtRegistration() {
        return countryAtRegistration;
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

    public void setPlannedRounds(Integer plannedRounds) {
        this.plannedRounds = plannedRounds;
    }

    public void setRankAtRegistration(String rankAtRegistration) {
        this.rankAtRegistration = rankAtRegistration;
    }

    public void setClubAtRegistration(String clubAtRegistration) {
        this.clubAtRegistration = clubAtRegistration;
    }

    public void setCountryAtRegistration(String countryAtRegistration) {
        this.countryAtRegistration = countryAtRegistration;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}