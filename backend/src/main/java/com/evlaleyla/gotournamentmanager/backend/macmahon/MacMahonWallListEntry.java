package com.evlaleyla.gotournamentmanager.backend.macmahon;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single entry of a MacMahon wall list.
 *
 * <p>This class is used as an intermediate data transfer object while parsing
 * wall list files before the data is persisted as tournament standings.</p>
 */
public class MacMahonWallListEntry {

    /**
     * The ranking position of the player in the wall list.
     */
    private Integer place;

    /**
     * The player name as provided by the imported wall list.
     */
    private String name;

    /**
     * The club identifier or club name from the imported wall list.
     */
    private String club;

    /**
     * The player level or rank shown in the wall list.
     */
    private String level;

    /**
     * The score value as provided by the MacMahon export.
     */
    private String score;

    /**
     * The per-round status values in display order.
     *
     * <p>Each entry represents the visible wall list value for one round.</p>
     */
    private List<String> roundStatuses = new ArrayList<>();

    /**
     * The points value of the player.
     */
    private Integer points;

    /**
     * The ScoreX tie-break value.
     */
    private String scoreX;

    /**
     * The SOS tie-break value.
     */
    private String sos;

    /**
     * The SOSOS tie-break value.
     */
    private String sosos;

    /**
     * Creates an empty wall list entry.
     */
    public MacMahonWallListEntry() {
    }

    /**
     * Creates a fully initialized wall list entry.
     *
     * @param place the ranking position
     * @param name the player name
     * @param club the club
     * @param level the level or rank
     * @param score the score value
     * @param roundStatuses the round status values
     * @param points the points value
     * @param scoreX the ScoreX value
     * @param sos the SOS value
     * @param sosos the SOSOS value
     */
    public MacMahonWallListEntry(Integer place,
                                 String name,
                                 String club,
                                 String level,
                                 String score,
                                 List<String> roundStatuses,
                                 Integer points,
                                 String scoreX,
                                 String sos,
                                 String sosos) {
        this.place = place;
        this.name = name;
        this.club = club;
        this.level = level;
        this.score = score;
        this.roundStatuses = roundStatuses;
        this.points = points;
        this.scoreX = scoreX;
        this.sos = sos;
        this.sosos = sosos;
    }

    /**
     * Returns the ranking position.
     *
     * @return the ranking position
     */
    public Integer getPlace() {
        return place;
    }

    /**
     * Sets the ranking position.
     *
     * @param place the ranking position
     */
    public void setPlace(Integer place) {
        this.place = place;
    }

    /**
     * Returns the player name.
     *
     * @return the player name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the player name.
     *
     * @param name the player name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the club.
     *
     * @return the club
     */
    public String getClub() {
        return club;
    }

    /**
     * Sets the club.
     *
     * @param club the club
     */
    public void setClub(String club) {
        this.club = club;
    }

    /**
     * Returns the player level or rank.
     *
     * @return the level or rank
     */
    public String getLevel() {
        return level;
    }

    /**
     * Sets the player level or rank.
     *
     * @param level the level or rank
     */
    public void setLevel(String level) {
        this.level = level;
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
     * Sets the score value.
     *
     * @param score the score value
     */
    public void setScore(String score) {
        this.score = score;
    }

    /**
     * Returns the round status values.
     *
     * @return the round status values
     */
    public List<String> getRoundStatuses() {
        return roundStatuses;
    }

    /**
     * Sets the round status values.
     *
     * @param roundStatuses the round status values
     */
    public void setRoundStatuses(List<String> roundStatuses) {
        this.roundStatuses = roundStatuses;
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
     * Sets the points value.
     *
     * @param points the points value
     */
    public void setPoints(Integer points) {
        this.points = points;
    }

    /**
     * Returns the ScoreX value.
     *
     * @return the ScoreX value
     */
    public String getScoreX() {
        return scoreX;
    }

    /**
     * Sets the ScoreX value.
     *
     * @param scoreX the ScoreX value
     */
    public void setScoreX(String scoreX) {
        this.scoreX = scoreX;
    }

    /**
     * Returns the SOS value.
     *
     * @return the SOS value
     */
    public String getSos() {
        return sos;
    }

    /**
     * Sets the SOS value.
     *
     * @param sos the SOS value
     */
    public void setSos(String sos) {
        this.sos = sos;
    }

    /**
     * Returns the SOSOS value.
     *
     * @return the SOSOS value
     */
    public String getSosos() {
        return sosos;
    }

    /**
     * Sets the SOSOS value.
     *
     * @param sosos the SOSOS value
     */
    public void setSosos(String sosos) {
        this.sosos = sosos;
    }
}