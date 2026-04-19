package com.evlaleyla.gotournamentmanager.backend.participant;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Der Vorname darf nicht leer sein.")
    private String firstName;

    @NotBlank(message = "Der Nachname darf nicht leer sein.")
    private String lastName;

    @NotBlank(message = "Die E-Mail-Adresse darf nicht leer sein.")
    @Email(message = "Bitte eine gültige E-Mail-Adresse eingeben.")
    @Column(nullable = false, unique = true)
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

    public Participant() {
    }

    public Participant(String firstName, String lastName, String email, String club, String country, String rank, LocalDate birthDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.club = club;
        this.country = country;
        this.rank = rank;
        this.birthDate = birthDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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

    public String getCountryDisplay() {
        return com.evlaleyla.gotournamentmanager.backend.CountryOptions.displayName(country);
    }
}