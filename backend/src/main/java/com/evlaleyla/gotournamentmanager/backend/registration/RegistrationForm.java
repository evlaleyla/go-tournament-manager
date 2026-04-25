package com.evlaleyla.gotournamentmanager.backend.registration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RegistrationForm {

    @NotNull(message = "Bitte ein Turnier auswählen.")
    private Long tournamentId;

    @NotNull(message = "Bitte einen Teilnehmer auswählen.")
    private Long participantId;

    @NotEmpty(message = "Bitte mindestens eine Runde auswählen.")
    private List<Integer> selectedRounds = new ArrayList<>();

    private String notes;

    public RegistrationForm() {
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

    public List<Integer> getSelectedRounds() {
        return selectedRounds;
    }

    public void setSelectedRounds(List<Integer> selectedRounds) {
        this.selectedRounds = selectedRounds != null ? selectedRounds : new ArrayList<>();
    }

    public Integer getPlannedRounds() {
        return selectedRounds != null ? selectedRounds.size() : 0;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}