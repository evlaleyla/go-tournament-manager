package com.evlaleyla.gotournamentmanager.backend.pairing;

import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * JPA entity representing a single pairing within a tournament round.
 *
 * <p>A pairing is uniquely identified within a tournament by the combination
 * of tournament, round number, and table number.</p>
 *
 * <p>The entity stores both operational data, such as assigned players,
 * result and handicap, and publication-related state for public visibility.</p>
 */
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

    /**
     * Tournament to which this pairing belongs.
     */
    @NotNull(message = "Das Turnier ist erforderlich.")
    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    /**
     * Round number within the tournament.
     */
    @NotNull(message = "Die Runde ist erforderlich.")
    @Min(value = 1, message = "Die Rundennummer muss mindestens 1 sein.")
    private Integer roundNumber;

    /**
     * Table number within the round.
     */
    @NotNull(message = "Die Tischnummer ist erforderlich.")
    @Min(value = 1, message = "Die Tischnummer muss mindestens 1 sein.")
    private Integer tableNumber;

    /**
     * Name of the player assigned to black.
     */
    @NotBlank(message = "Der Name von Schwarz ist erforderlich.")
    private String blackPlayer;

    /**
     * Name of the player assigned to white.
     */
    @NotBlank(message = "Der Name von Weiß ist erforderlich.")
    private String whitePlayer;

    /**
     * Result code of the pairing.
     *
     * <p>Allowed values:</p>
     * <ul>
     *     <li>{@code null} = open / not yet decided</li>
     *     <li>{@code B} = black wins</li>
     *     <li>{@code W} = white wins</li>
     *     <li>{@code J} = jigo / draw</li>
     *     <li>{@code L} = both lose</li>
     *     <li>{@code D} = both win</li>
     * </ul>
     */
    private String result;

    /**
     * Optional handicap notation imported from MacMahon.
     */
    @Column(length = 20)
    private String handicap;

    /**
     * Indicates whether the pairing represents a bye game.
     */
    @Column(name = "bye_game", nullable = false)
    private boolean bye;

    /**
     * Indicates whether the pairing is visible in the public view.
     */
    @Column(name = "published", nullable = false)
    private boolean published = false;

    /**
     * Default constructor required by JPA.
     */
    public Pairing() {
    }

    /**
     * Convenience constructor for a basic pairing without handicap, bye or publication state.
     */
    public Pairing(Tournament tournament,
                   Integer roundNumber,
                   Integer tableNumber,
                   String blackPlayer,
                   String whitePlayer,
                   String result) {
        this(tournament, roundNumber, tableNumber, blackPlayer, whitePlayer, result, null, false, false);
    }

    /**
     * Convenience constructor for a pairing with handicap and bye information.
     * The pairing is initialized as unpublished.
     */
    public Pairing(Tournament tournament,
                   Integer roundNumber,
                   Integer tableNumber,
                   String blackPlayer,
                   String whitePlayer,
                   String result,
                   String handicap,
                   boolean bye) {
        this(tournament, roundNumber, tableNumber, blackPlayer, whitePlayer, result, handicap, bye, false);
    }

    /**
     * Full constructor for creating a pairing with all supported properties.
     */
    public Pairing(Tournament tournament,
                   Integer roundNumber,
                   Integer tableNumber,
                   String blackPlayer,
                   String whitePlayer,
                   String result,
                   String handicap,
                   boolean bye,
                   boolean published) {
        this.tournament = tournament;
        this.roundNumber = roundNumber;
        this.tableNumber = tableNumber;
        this.blackPlayer = blackPlayer;
        this.whitePlayer = whitePlayer;
        this.result = result;
        this.handicap = handicap;
        this.bye = bye;
        this.published = published;
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

    public String getHandicap() {
        return handicap;
    }

    public boolean isBye() {
        return bye;
    }

    public boolean isPublished() {
        return published;
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

    public void setHandicap(String handicap) {
        this.handicap = handicap;
    }

    public void setBye(boolean bye) {
        this.bye = bye;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    /**
     * Returns a human-readable German label for the internal result code.
     *
     * @return display label for the current result state
     */
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

    /**
     * Returns a printable handicap value.
     *
     * @return handicap or "-" if no handicap is set
     */
    public String getHandicapDisplay() {
        if (handicap == null || handicap.isBlank()) {
            return "-";
        }

        return handicap;
    }

    /**
     * Returns a German yes/no label for the bye flag.
     *
     * @return "Ja" if this is a bye game, otherwise "Nein"
     */
    public String getByeDisplay() {
        return bye ? "Ja" : "Nein";
    }

    /**
     * Returns a human-readable German label for the publication state.
     *
     * @return publication status label
     */
    public String getPublicationStatusDisplay() {
        return published ? "Veröffentlicht" : "Nicht veröffentlicht";
    }
}