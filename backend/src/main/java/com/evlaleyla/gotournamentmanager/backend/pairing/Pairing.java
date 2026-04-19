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

    /**
     * Erlaubte Werte:
     * null = offen
     * B = Schwarz gewinnt
     * W = Weiß gewinnt
     * J = Jigo / Unentschieden
     * L = beide verlieren
     * D = beide gewinnen
     */
    private String result;

    @Column(length = 20)
    private String handicap;

    @Column(name = "bye_game", nullable = false)
    private boolean bye;

    public Pairing() {
    }

    public Pairing(Tournament tournament,
                   Integer roundNumber,
                   Integer tableNumber,
                   String blackPlayer,
                   String whitePlayer,
                   String result) {
        this(tournament, roundNumber, tableNumber, blackPlayer, whitePlayer, result, null, false);
    }

    public Pairing(Tournament tournament,
                   Integer roundNumber,
                   Integer tableNumber,
                   String blackPlayer,
                   String whitePlayer,
                   String result,
                   String handicap,
                   boolean bye) {
        this.tournament = tournament;
        this.roundNumber = roundNumber;
        this.tableNumber = tableNumber;
        this.blackPlayer = blackPlayer;
        this.whitePlayer = whitePlayer;
        this.result = result;
        this.handicap = handicap;
        this.bye = bye;
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

        return switch (result) {
            case "B" -> "Schwarz gewinnt";
            case "W" -> "Weiß gewinnt";
            case "J" -> "Jigo";
            case "L" -> "Beide verlieren";
            case "D" -> "Beide gewinnen";
            default -> result;
        };
    }

    public String getHandicap() {
        return handicap;
    }

    public void setHandicap(String handicap) {
        this.handicap = handicap;
    }

    public boolean isBye() {
        return bye;
    }

    public void setBye(boolean bye) {
        this.bye = bye;
    }

    public String getHandicapDisplay() {
        if (handicap == null || handicap.isBlank()) {
            return "-";
        }

        return handicap;
    }

    public String getByeDisplay() {
        return bye ? "Ja" : "Nein";
    }
}