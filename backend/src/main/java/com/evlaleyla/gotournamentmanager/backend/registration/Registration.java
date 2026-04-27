package com.evlaleyla.gotournamentmanager.backend.registration;

import com.evlaleyla.gotournamentmanager.backend.participant.Participant;
import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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

    @NotEmpty(message = "Mindestens eine ausgewählte Runde ist erforderlich.")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "registration_selected_rounds",
            joinColumns = @JoinColumn(name = "registration_id")
    )
    @Column(name = "round_number", nullable = false)
    private Set<Integer> selectedRounds = new LinkedHashSet<>();

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
                        Set<Integer> selectedRounds,
                        String rankAtRegistration,
                        String clubAtRegistration,
                        String countryAtRegistration,
                        String notes) {
        this.tournament = tournament;
        this.participant = participant;
        this.registrationDate = registrationDate;
        setSelectedRounds(selectedRounds);
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

    public Set<Integer> getSelectedRounds() {
        return selectedRounds.stream()
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
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

    @Transient
    public Integer getSelectedRoundCount() {
        return selectedRounds != null ? selectedRounds.size() : 0;
    }

    @Transient
    public boolean isPlayingInRound(int roundNumber) {
        return selectedRounds != null && selectedRounds.contains(roundNumber);
    }

    @Transient
    public String getSelectedRoundsDisplay() {
        if (selectedRounds == null || selectedRounds.isEmpty()) {
            return "";
        }

        return selectedRounds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
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

    public void setSelectedRounds(Set<Integer> selectedRounds) {
        this.selectedRounds.clear();

        if (selectedRounds != null) {
            this.selectedRounds.addAll(new TreeSet<>(selectedRounds));
        }
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

    @Transient
    public String getSelectedRoundsDisplayShort() {
        if (selectedRounds == null || selectedRounds.isEmpty()) {
            return "-";
        }

        Integer totalRounds = tournament != null ? tournament.getNumberOfRounds() : null;

        if (totalRounds != null && totalRounds > 0) {
            boolean allRoundsSelected = selectedRounds.size() == totalRounds;

            if (allRoundsSelected) {
                for (int round = 1; round <= totalRounds; round++) {
                    if (!selectedRounds.contains(round)) {
                        allRoundsSelected = false;
                        break;
                    }
                }
            }

            if (allRoundsSelected) {
                return "Alle Runden";
            }
        }

        return selectedRounds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }
}