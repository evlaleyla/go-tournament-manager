package com.evlaleyla.gotournamentmanager.backend.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SelfRegistrationForm {

    @NotBlank(message = "Der Vorname darf nicht leer sein.")
    private String firstName;

    @NotBlank(message = "Der Nachname darf nicht leer sein.")
    private String lastName;

    @NotBlank(message = "Die E-Mail-Adresse darf nicht leer sein.")
    @Email(message = "Bitte eine gültige E-Mail-Adresse eingeben.")
    private String email;

    private String club;

    private String country;

    @NotBlank(message = "Der Spielrang darf nicht leer sein.")
    @jakarta.validation.constraints.Pattern(
            regexp = "(?i)^([1-9]|[12][0-9]|30)\\s*(k|kyu|d|dan|p|pro)$",
            message = "Bitte einen gültigen Go-Rang eingeben, z. B. 10k, 1d oder 2p."
    )
    private String rank;

    @PastOrPresent(message = "Das Geburtsdatum darf nicht in der Zukunft liegen.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    @NotEmpty(message = "Bitte mindestens eine Runde auswählen.")
    private List<Integer> selectedRounds = new ArrayList<>();

    private String notes;

    public SelfRegistrationForm() {
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getClub() {
        return club;
    }

    public String getCountry() {
        return country;
    }

    public String getRank() {
        return rank;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public List<Integer> getSelectedRounds() {
        return selectedRounds;
    }

    public void setSelectedRounds(List<Integer> selectedRounds) {
        this.selectedRounds = selectedRounds != null ? selectedRounds : new ArrayList<>();
    }

    public Integer getSelectedRoundCount() {
        return selectedRounds != null ? selectedRounds.size() : 0;
    }

    public String getNotes() {
        return notes;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setClub(String club) {
        this.club = club;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}