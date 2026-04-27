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

/**
 * Entity representing a participant's registration for a specific tournament.
 *
 * <p>A registration stores both the relation to the participant and tournament
 * and a snapshot of relevant participant-related data at the time of registration
 * such as rank, club and country.</p>
 *
 * <p>Additionally, the registration contains the rounds selected for participation,
 * optional notes and several derived helper methods for display purposes.</p>
 */
@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_id", "participant_id"})
)
public class Registration {

    /**
     * Technical primary key of the registration.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tournament to which this registration belongs.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    /**
     * Participant who is registered for the tournament.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    /**
     * Date on which the registration was created.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registrationDate;

    /**
     * Set of round numbers in which the participant intends to play.
     *
     * <p>The values are stored in a separate collection table and are kept sorted
     * when written through the setter.</p>
     */
    @NotEmpty(message = "Mindestens eine ausgewählte Runde ist erforderlich.")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "registration_selected_rounds",
            joinColumns = @JoinColumn(name = "registration_id")
    )
    @Column(name = "round_number", nullable = false)
    private Set<Integer> selectedRounds = new LinkedHashSet<>();

    /**
     * Participant rank at the time of registration.
     *
     * <p>This is intentionally stored redundantly to preserve the registration
     * state even if the participant master data changes later.</p>
     */
    @NotBlank(message = "Der Rang ist erforderlich.")
    private String rankAtRegistration;

    /**
     * Participant club at the time of registration.
     */
    private String clubAtRegistration;

    /**
     * Participant country at the time of registration.
     */
    private String countryAtRegistration;

    /**
     * Optional organizer notes related to this registration.
     */
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

    /**
     * Returns the selected rounds as a sorted, stable set.
     *
     * @return sorted selected round numbers
     */
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

    /**
     * Returns the number of selected rounds.
     *
     * @return number of selected rounds, or {@code 0} if none exist
     */
    @Transient
    public Integer getSelectedRoundCount() {
        return selectedRounds != null ? selectedRounds.size() : 0;
    }

    /**
     * Checks whether the participant is registered to play in the given round.
     *
     * @param roundNumber the round number to check
     * @return {@code true} if the round is selected, otherwise {@code false}
     */
    @Transient
    public boolean isPlayingInRound(int roundNumber) {
        return selectedRounds != null && selectedRounds.contains(roundNumber);
    }

    /**
     * Returns a comma-separated display string of all selected rounds.
     *
     * @return formatted round list, or an empty string if no rounds are selected
     */
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

    /**
     * Replaces the selected rounds with a sorted copy of the provided set.
     *
     * <p>A {@link TreeSet} is used internally to enforce ascending order and
     * uniqueness before the values are stored in the entity field.</p>
     *
     * @param selectedRounds the new selected rounds
     */
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

    /**
     * Returns a compact display representation of the selected rounds.
     *
     * <p>If all tournament rounds are selected, the method returns
     * {@code "Alle Runden"}. Otherwise, it returns the selected round numbers
     * as a comma-separated list. If no rounds are selected, {@code "-"} is returned.</p>
     *
     * @return short display string for the selected rounds
     */
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