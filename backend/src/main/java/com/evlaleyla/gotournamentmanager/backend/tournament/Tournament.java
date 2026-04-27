package com.evlaleyla.gotournamentmanager.backend.tournament;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Der Turniername darf nicht leer sein.")
    private String name;

    private String location;

    @NotNull(message = "Das Startdatum ist erforderlich.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "Das Enddatum ist erforderlich.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registrationDeadline;

    @NotNull(message = "Die Anzahl der Runden ist erforderlich.")
    @Min(value = 1, message = "Die Anzahl der Runden muss mindestens 1 sein.")
    private Integer numberOfRounds;

    private String description;

    @NotNull(message = "Der Status ist erforderlich.")
    @Enumerated(EnumType.STRING)
    private TournamentStatus status;

    private LocalDateTime lastWallListImportAt;


    @Column(nullable = false)
    private boolean wallListPublished = false;

    private LocalDateTime wallListPublishedAt;

    public Tournament() {
    }

    public Tournament(String name, String location, LocalDate startDate, LocalDate endDate,
                      LocalDate registrationDeadline, Integer numberOfRounds,
                      String description, TournamentStatus status) {
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

    public LocalDateTime getLastWallListImportAt() {
        return lastWallListImportAt;
    }

    public void setLastWallListImportAt(LocalDateTime lastWallListImportAt) {
        this.lastWallListImportAt = lastWallListImportAt;
    }

    public boolean isWallListPublished() {
        return wallListPublished;
    }

    public void setWallListPublished(boolean wallListPublished) {
        this.wallListPublished = wallListPublished;
    }

    public LocalDateTime getWallListPublishedAt() {
        return wallListPublishedAt;
    }

    public void setWallListPublishedAt(LocalDateTime wallListPublishedAt) {
        this.wallListPublishedAt = wallListPublishedAt;
    }
}