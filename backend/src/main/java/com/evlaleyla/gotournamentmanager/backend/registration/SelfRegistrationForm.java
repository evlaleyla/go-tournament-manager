package com.evlaleyla.gotournamentmanager.backend.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Form object for public self-registration.
 *
 * <p>This class is used to bind and validate the data entered by a participant
 * on the public registration page. It is intentionally kept simple because it
 * only transports form data between the view and the controller.</p>
 */
public class SelfRegistrationForm {

    /**
     * Participant first name.
     */
    @NotBlank(message = "Der Vorname darf nicht leer sein.")
    private String firstName;

    /**
     * Participant last name.
     */
    @NotBlank(message = "Der Nachname darf nicht leer sein.")
    private String lastName;

    /**
     * Participant email address.
     */
    @NotBlank(message = "Die E-Mail-Adresse darf nicht leer sein.")
    @Email(message = "Bitte eine gültige E-Mail-Adresse eingeben.")
    private String email;

    /**
     * Optional club of the participant.
     */
    private String club;

    /**
     * Optional country of the participant.
     */
    private String country;

    /**
     * Go rank entered by the participant.
     *
     * <p>Allowed examples: 10k, 1d, 2p, 5 kyu, 3 dan, 1 pro.</p>
     */
    @NotBlank(message = "Der Spielrang darf nicht leer sein.")
    @jakarta.validation.constraints.Pattern(
            regexp = "(?i)^([1-9]|[12][0-9]|30)\\s*(k|kyu|d|dan|p|pro)$",
            message = "Bitte einen gültigen Go-Rang eingeben, z. B. 10k, 1d oder 2p."
    )
    private String rank;

    /**
     * Optional birth date of the participant.
     *
     * <p>The date may not lie in the future.</p>
     */
    @PastOrPresent(message = "Das Geburtsdatum darf nicht in der Zukunft liegen.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    /**
     * List of tournament rounds selected by the participant.
     *
     * <p>At least one round must be selected.</p>
     */
    @NotEmpty(message = "Bitte mindestens eine Runde auswählen.")
    private List<Integer> selectedRounds = new ArrayList<>();

    /**
     * Optional free-text notes entered during registration.
     */
    private String notes;

    /**
     * Default constructor required for Spring form binding.
     */
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

    /**
     * Replaces the selected rounds list.
     *
     * <p>If {@code null} is passed, an empty list is stored instead to avoid
     * null handling in other parts of the application.</p>
     *
     * @param selectedRounds the selected rounds
     */
    public void setSelectedRounds(List<Integer> selectedRounds) {
        this.selectedRounds = selectedRounds != null ? selectedRounds : new ArrayList<>();
    }

    /**
     * Returns the number of selected rounds.
     *
     * @return number of selected rounds, or {@code 0} if the list is empty
     */
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