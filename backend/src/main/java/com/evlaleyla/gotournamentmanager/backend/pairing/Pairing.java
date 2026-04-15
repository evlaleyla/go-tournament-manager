package com.evlaleyla.gotournamentmanager.backend.pairing;

import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"tournament_id", "round_number", "table_number"}
        )
)
public class Pairing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Das Turnier ist erforderlich.")
    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @NotNull(message = "Die Runde ist erforderlich.")
    @Min(value = 1, message = "Die Rundennummer muss mindestens 1 sein.")
    private Integer roundNumber;

    @NotNull(message = "Die Tischnummer ist erforderlich.")
    @Min(value = 1, message = "Die Tischnummer muss mindestens 1 sein.")
    private Integer tableNumber;

    @NotBlank(message = "Der Name von Schwarz ist erforderlich.")
    private String blackPlayer;

    @NotBlank(message = "Der Name von Weiß ist erforderlich.")
    private String whitePlayer;

    private String result;

    public Pairing() {
    }

    public Pairing(Tournament tournament, Integer roundNumber, Integer tableNumber,
                   String blackPlayer, String whitePlayer, String result) {
        this.tournament = tournament;
        this.roundNumber = roundNumber;
        this.tableNumber = tableNumber;
        this.blackPlayer = blackPlayer;
        this.whitePlayer = whitePlayer;
        this.result = result;
    }

    public Long getId() {
        return id;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public Integer getTableNumber() {
        return tableNumber;
    }

    public String getBlackPlayer() {
        return blackPlayer;
    }

    public String getWhitePlayer() {
        return whitePlayer;
    }

    public String getResult() {
        return result;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public void setTableNumber(Integer tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setBlackPlayer(String blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public void setWhitePlayer(String whitePlayer) {
        this.whitePlayer = whitePlayer;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResultDisplay() {
        if (result == null || result.isBlank()) {
            return "offen";
        }

        if ("0".equals(result)) {
            return "Jigo";
        }

        if (result.startsWith("B+")) {
            return buildReadableResult("Schwarz", result.substring(2));
        }

        if (result.startsWith("W+")) {
            return buildReadableResult("Weiß", result.substring(2));
        }

        return result;
    }

    private String buildReadableResult(String winner, String suffix) {
        return switch (suffix) {
            case "R" -> winner + " gewinnt durch Aufgabe";
            case "T" -> winner + " gewinnt auf Zeit";
            case "F" -> winner + " gewinnt kampflos";
            default -> winner + " gewinnt mit " + suffix.replace(".", ",") + " Punkten";
        };
    }
}