package com.evlaleyla.gotournamentmanager.backend.macmahon;

import java.util.ArrayList;
import java.util.List;

public class MacMahonWallListEntry {

    private Integer place;
    private String name;
    private String club;
    private String level;
    private String score;
    private List<String> roundStatuses = new ArrayList<>();
    private Integer points;
    private String scoreX;
    private String sos;
    private String sosos;

    public MacMahonWallListEntry() {
    }

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

    public Integer getPlace() {
        return place;
    }

    public void setPlace(Integer place) {
        this.place = place;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClub() {
        return club;
    }

    public void setClub(String club) {
        this.club = club;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public List<String> getRoundStatuses() {
        return roundStatuses;
    }

    public void setRoundStatuses(List<String> roundStatuses) {
        this.roundStatuses = roundStatuses;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getScoreX() {
        return scoreX;
    }

    public void setScoreX(String scoreX) {
        this.scoreX = scoreX;
    }

    public String getSos() {
        return sos;
    }

    public void setSos(String sos) {
        this.sos = sos;
    }

    public String getSosos() {
        return sosos;
    }

    public void setSosos(String sosos) {
        this.sosos = sosos;
    }
}