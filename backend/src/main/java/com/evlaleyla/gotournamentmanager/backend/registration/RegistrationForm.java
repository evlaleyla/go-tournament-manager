package com.evlaleyla.gotournamentmanager.backend.registration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Form backing object for creating and updating registrations
 * in the administrative area.
 *
 * <p>This class is used by Spring MVC to bind form input data
 * before it is validated and processed by the controller/service layer.</p>
 */
public class RegistrationForm {

    /**
     * Selected tournament identifier.
     */
    @NotNull(message = "Bitte ein Turnier auswählen.")
    private Long tournamentId;

    /**
     * Selected participant identifier.
     */
    @NotNull(message = "Bitte einen Teilnehmer auswählen.")
    private Long participantId;

    /**
     * List of selected round numbers.
     *
     * <p>At least one round must be selected. The list is initialized
     * eagerly to avoid null handling in form binding and view rendering.</p>
     */
    @NotEmpty(message = "Bitte mindestens eine Runde auswählen.")
    private List<Integer> selectedRounds = new ArrayList<>();

    /**
     * Optional administrative notes for the registration.
     */
    private String notes;

    public RegistrationForm() {
        // Default constructor required for Spring form binding.
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(Long tournamentId) {
        this.tournamentId = tournamentId;
    }

    public Long getParticipantId() {
        return participantId;
    }

    public void setParticipantId(Long participantId) {
        this.participantId = participantId;
    }

    /**
     * Returns the currently selected rounds.
     *
     * @return never {@code null}
     */
    public List<Integer> getSelectedRounds() {
        return selectedRounds;
    }

    /**
     * Sets the selected rounds.
     *
     * <p>If the incoming list is {@code null}, an empty list is assigned
     * to keep the form object in a consistent non-null state.</p>
     *
     * @param selectedRounds selected round numbers
     */
    public void setSelectedRounds(List<Integer> selectedRounds) {
        this.selectedRounds = selectedRounds != null ? selectedRounds : new ArrayList<>();
    }

    /**
     * Returns the number of selected rounds.
     *
     * @return number of selected rounds, or {@code 0} if none exist
     */
    public Integer getSelectedRoundCount() {
        return selectedRounds != null ? selectedRounds.size() : 0;
    }

    public String getNotes() {
        return notes;
    }

    /**
     * Sets optional notes for the registration.
     *
     * @param notes free-text notes entered in the form
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }
}