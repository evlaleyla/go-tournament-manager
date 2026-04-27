package com.evlaleyla.gotournamentmanager.backend.macmahon;

import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Arrays;
import java.util.List;

/**
 * Persistence entity representing one imported tournament standing entry.
 *
 * <p>This entity stores the current ranking snapshot of a tournament as imported
 * from a MacMahon wall list. Each row corresponds to one participant entry in
 * the imported standing table.</p>
 */
@Entity
@Table(name = "tournament_standing")
public class TournamentStanding {

    /**
     * Delimiter used to serialize round status values into a single database column.
     */
    private static final String ROUND_STATUS_DELIMITER = "|";

    /**
     * Regular expression used to split serialized round status values.
     */
    private static final String ROUND_STATUS_SPLIT_REGEX = "\\|";

    /**
     * Technical primary key of the standing entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tournament to which this standing entry belongs.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    /**
     * Ranking position in the imported wall list.
     */
    private Integer place;

    /**
     * Participant name as provided by the imported source file.
     */
    private String name;

    /**
     * Club value from the imported wall list.
     */
    private String club;

    /**
     * Player level or rank from the imported wall list.
     */
    private String level;

    /**
     * Score value from the imported wall list.
     */
    private String score;

    /**
     * Serialized representation of the per-round status values.
     *
     * <p>The values are stored in a single database column and converted to a list
     * via {@link #getRoundStatuses()} and {@link #setRoundStatuses(List)}.</p>
     */
    @Column(length = 2000)
    private String roundStatusesSerialized;

    /**
     * Points value from the imported wall list.
     */
    private Integer points;

    /**
     * ScoreX tie-break value.
     */
    private String scoreX;

    /**
     * SOS tie-break value.
     */
    private String sos;

    /**
     * SOSOS tie-break value.
     */
    private String sosos;

    /**
     * Default constructor required by JPA.
     */
    public TournamentStanding() {
    }

    /**
     * Returns the technical identifier.
     *
     * @return the entity id
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the related tournament.
     *
     * @return the tournament
     */
    public Tournament getTournament() {
        return tournament;
    }

    /**
     * Returns the ranking place.
     *
     * @return the ranking place
     */
    public Integer getPlace() {
        return place;
    }

    /**
     * Returns the participant name.
     *
     * @return the participant name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the club value.
     *
     * @return the club value
     */
    public String getClub() {
        return club;
    }

    /**
     * Returns the level or rank.
     *
     * @return the level or rank
     */
    public String getLevel() {
        return level;
    }

    /**
     * Returns the score value.
     *
     * @return the score value
     */
    public String getScore() {
        return score;
    }

    /**
     * Returns the serialized round status representation.
     *
     * @return the serialized round status string
     */
    public String getRoundStatusesSerialized() {
        return roundStatusesSerialized;
    }

    /**
     * Returns the points value.
     *
     * @return the points value
     */
    public Integer getPoints() {
        return points;
    }

    /**
     * Returns the ScoreX tie-break value.
     *
     * @return the ScoreX value
     */
    public String getScoreX() {
        return scoreX;
    }

    /**
     * Returns the SOS tie-break value.
     *
     * @return the SOS value
     */
    public String getSos() {
        return sos;
    }

    /**
     * Returns the SOSOS tie-break value.
     *
     * @return the SOSOS value
     */
    public String getSosos() {
        return sosos;
    }

    /**
     * Sets the technical identifier.
     *
     * @param id the entity id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Sets the related tournament.
     *
     * @param tournament the tournament
     */
    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    /**
     * Sets the ranking place.
     *
     * @param place the ranking place
     */
    public void setPlace(Integer place) {
        this.place = place;
    }

    /**
     * Sets the participant name.
     *
     * @param name the participant name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the club value.
     *
     * @param club the club value
     */
    public void setClub(String club) {
        this.club = club;
    }

    /**
     * Sets the level or rank.
     *
     * @param level the level or rank
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * Sets the score value.
     *
     * @param score the score value
     */
    public void setScore(String score) {
        this.score = score;
    }

    /**
     * Sets the serialized round status representation directly.
     *
     * @param roundStatusesSerialized the serialized round status string
     */
    public void setRoundStatusesSerialized(String roundStatusesSerialized) {
        this.roundStatusesSerialized = roundStatusesSerialized;
    }

    /**
     * Sets the points value.
     *
     * @param points the points value
     */
    public void setPoints(Integer points) {
        this.points = points;
    }

    /**
     * Sets the ScoreX tie-break value.
     *
     * @param scoreX the ScoreX value
     */
    public void setScoreX(String scoreX) {
        this.scoreX = scoreX;
    }

    /**
     * Sets the SOS tie-break value.
     *
     * @param sos the SOS value
     */
    public void setSos(String sos) {
        this.sos = sos;
    }

    /**
     * Sets the SOSOS tie-break value.
     *
     * @param sosos the SOSOS value
     */
    public void setSosos(String sosos) {
        this.sosos = sosos;
    }

    /**
     * Returns the round status values as a list.
     *
     * <p>If no round statuses are stored, an empty list is returned.</p>
     *
     * @return the deserialized round status values
     */
    public List<String> getRoundStatuses() {
        if (roundStatusesSerialized == null || roundStatusesSerialized.isBlank()) {
            return List.of();
        }

        return Arrays.asList(roundStatusesSerialized.split(ROUND_STATUS_SPLIT_REGEX, -1));
    }

    /**
     * Serializes and stores the round status values.
     *
     * <p>If the provided list is {@code null} or empty, an empty string is stored.</p>
     *
     * @param roundStatuses the round status values to serialize
     */
    public void setRoundStatuses(List<String> roundStatuses) {
        if (roundStatuses == null || roundStatuses.isEmpty()) {
            this.roundStatusesSerialized = "";
            return;
        }

        this.roundStatusesSerialized = String.join(ROUND_STATUS_DELIMITER, roundStatuses);
    }
}