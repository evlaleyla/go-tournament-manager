package com.evlaleyla.gotournamentmanager.backend.tournament;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a Go tournament.
 *
 * <p>This entity stores the core administrative information of a tournament,
 * including scheduling data, registration settings, round count, status,
 * and metadata related to imported and published wall list standings.</p>
 */
@Entity
public class Tournament {

    /**
     * Technical primary key of the tournament.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Display name of the tournament.
     */
    @NotBlank(message = "Der Turniername darf nicht leer sein.")
    private String name;

    /**
     * Optional tournament location.
     */
    private String location;

    /**
     * Start date of the tournament.
     */
    @NotNull(message = "Das Startdatum ist erforderlich.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    /**
     * End date of the tournament.
     */
    @NotNull(message = "Das Enddatum ist erforderlich.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    /**
     * Optional registration deadline.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registrationDeadline;

    /**
     * Total number of tournament rounds.
     */
    @NotNull(message = "Die Anzahl der Runden ist erforderlich.")
    @Min(value = 1, message = "Die Anzahl der Runden muss mindestens 1 sein.")
    private Integer numberOfRounds;

    /**
     * Optional public or internal tournament description.
     */
    private String description;

    /**
     * Current lifecycle status of the tournament.
     */
    @NotNull(message = "Der Status ist erforderlich.")
    @Enumerated(EnumType.STRING)
    private TournamentStatus status;

    /**
     * Timestamp of the most recent wall list import.
     */
    private LocalDateTime lastWallListImportAt;

    /**
     * Indicates whether the imported wall list is currently publicly visible.
     */
    @Column(nullable = false)
    private boolean wallListPublished = false;

    /**
     * Timestamp of the last wall list publication.
     */
    private LocalDateTime wallListPublishedAt;

    /**
     * Default constructor required by JPA.
     */
    public Tournament() {
    }

    /**
     * Creates a tournament with its main business fields.
     *
     * @param name                 tournament name
     * @param location             tournament location
     * @param startDate            start date
     * @param endDate              end date
     * @param registrationDeadline optional registration deadline
     * @param numberOfRounds       total number of rounds
     * @param description          optional description
     * @param status               tournament status
     */
    public Tournament(String name,
                      String location,
                      LocalDate startDate,
                      LocalDate endDate,
                      LocalDate registrationDeadline,
                      Integer numberOfRounds,
                      String description,
                      TournamentStatus status) {
        this.name = name;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.registrationDeadline = registrationDeadline;
        this.numberOfRounds = numberOfRounds;
        this.description = description;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getRegistrationDeadline() {
        return registrationDeadline;
    }

    public Integer getNumberOfRounds() {
        return numberOfRounds;
    }

    public String getDescription() {
        return description;
    }

    public TournamentStatus getStatus() {
        return status;
    }

    public LocalDateTime getLastWallListImportAt() {
        return lastWallListImportAt;
    }

    public boolean isWallListPublished() {
        return wallListPublished;
    }

    public LocalDateTime getWallListPublishedAt() {
        return wallListPublishedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setRegistrationDeadline(LocalDate registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public void setNumberOfRounds(Integer numberOfRounds) {
        this.numberOfRounds = numberOfRounds;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(TournamentStatus status) {
        this.status = status;
    }

    public void setLastWallListImportAt(LocalDateTime lastWallListImportAt) {
        this.lastWallListImportAt = lastWallListImportAt;
    }

    public void setWallListPublished(boolean wallListPublished) {
        this.wallListPublished = wallListPublished;
    }

    public void setWallListPublishedAt(LocalDateTime wallListPublishedAt) {
        this.wallListPublishedAt = wallListPublishedAt;
    }
}