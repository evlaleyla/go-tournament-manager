package com.evlaleyla.gotournamentmanager.backend.registration;

import jakarta.validation.constraints.NotNull;

public class RegistrationForm {

    @NotNull(message = "Bitte ein Turnier auswählen.")
    private Long tournamentId;

    @NotNull(message = "Bitte einen Teilnehmer auswählen.")
    private Long participantId;

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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}