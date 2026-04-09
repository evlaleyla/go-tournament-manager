package com.evlaleyla.gotournamentmanager.backend.tournament;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

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

    private String description;

    @NotNull(message = "Der Status ist erforderlich.")
    @Enumerated(EnumType.STRING)
    private TournamentStatus status;

    public Tournament() {
    }

    public Tournament(String name, String location, LocalDate startDate, LocalDate endDate,
                      LocalDate registrationDeadline,
                      String description, TournamentStatus status) {
        this.name = name;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.registrationDeadline = registrationDeadline;
        this.description = description;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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


    public String getDescription() {
        return description;
    }

    public TournamentStatus getStatus() {
        return status;
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

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(TournamentStatus status) {
        this.status = status;
    }
}