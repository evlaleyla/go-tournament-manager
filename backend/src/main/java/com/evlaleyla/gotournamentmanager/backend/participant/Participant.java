package com.evlaleyla.gotournamentmanager.backend.participant;

import com.evlaleyla.gotournamentmanager.backend.CountryOptions;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Entity representing a participant of one or more Go tournaments.
 *
 * <p>A participant stores the person's master data that can later be reused
 * for registrations in different tournaments.</p>
 */
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

    /**
     * Club affiliation of the participant.
     *
     * <p>This field may be empty if no club is specified.</p>
     */
    private String club;

    /**
     * Country code or country value of the participant.
     *
     * <p>The application normalizes this value via {@link CountryOptions}.</p>
     */
    private String country;

    @NotBlank(message = "Der Spielrang darf nicht leer sein.")
    @Pattern(
            regexp = "(?i)^([1-9]|[12][0-9]|30)\\s*(k|kyu|d|dan|p|pro)$",
            message = "Bitte einen gültigen Go-Rang eingeben, z. B. 10k, 1d oder 2p."
    )
    private String rank;

    @PastOrPresent(message = "Das Geburtsdatum darf nicht in der Zukunft liegen.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    public Participant() {
        // Required by JPA
    }

    public Participant(String firstName,
                       String lastName,
                       String email,
                       String club,
                       String country,
                       String rank,
                       LocalDate birthDate) {
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

    /**
     * Returns the participant's full name in the format "firstName lastName".
     *
     * @return normalized full name string
     */
    public String getFullName() {
        String normalizedFirstName = firstName == null ? "" : firstName.trim();
        String normalizedLastName = lastName == null ? "" : lastName.trim();

        return (normalizedFirstName + " " + normalizedLastName)
                .trim()
                .replaceAll("\\s+", " ");
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getClub() {
        return club;
    }

    public void setClub(String club) {
        this.club = club;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    /**
     * Returns the display name of the participant's country.
     *
     * @return localized display value for the stored country
     */
    public String getCountryDisplay() {
        return CountryOptions.displayName(country);
    }
}