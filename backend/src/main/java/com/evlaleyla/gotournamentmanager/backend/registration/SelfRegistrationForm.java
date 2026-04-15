package com.evlaleyla.gotournamentmanager.backend.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

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
    private String rank;

    @PastOrPresent(message = "Das Geburtsdatum darf nicht in der Zukunft liegen.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    @NotNull(message = "Bitte die geplante Rundenzahl angeben.")
    @Min(value = 1, message = "Die geplante Rundenzahl muss mindestens 1 sein.")
    private Integer plannedRounds;

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

    public Integer getPlannedRounds() {
        return plannedRounds;
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

    public void setPlannedRounds(Integer plannedRounds) {
        this.plannedRounds = plannedRounds;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}