package com.evlaleyla.gotournamentmanager.backend.macmahon;

import com.evlaleyla.gotournamentmanager.backend.tournament.Tournament;
import jakarta.persistence.*;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "tournament_standing")
public class TournamentStanding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    private Integer place;
    private String name;
    private String club;
    private String level;
    private String score;

    @Column(length = 2000)
    private String roundStatusesSerialized;

    private Integer points;
    private String scoreX;
    private String sos;
    private String sosos;

    public TournamentStanding() {
    }

    public Long getId() {
        return id;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public Integer getPlace() {
        return place;
    }

    public String getName() {
        return name;
    }

    public String getClub() {
        return club;
    }

    public String getLevel() {
        return level;
    }

    public String getScore() {
        return score;
    }

    public String getRoundStatusesSerialized() {
        return roundStatusesSerialized;
    }

    public Integer getPoints() {
        return points;
    }

    public String getScoreX() {
        return scoreX;
    }

    public String getSos() {
        return sos;
    }

    public String getSosos() {
        return sosos;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public void setPlace(Integer place) {
        this.place = place;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setClub(String club) {
        this.club = club;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public void setRoundStatusesSerialized(String roundStatusesSerialized) {
        this.roundStatusesSerialized = roundStatusesSerialized;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public void setScoreX(String scoreX) {
        this.scoreX = scoreX;
    }

    public void setSos(String sos) {
        this.sos = sos;
    }

    public void setSosos(String sosos) {
        this.sosos = sosos;
    }

    public List<String> getRoundStatuses() {
        if (roundStatusesSerialized == null || roundStatusesSerialized.isBlank()) {
            return List.of();
        }

        return Arrays.asList(roundStatusesSerialized.split("\\|", -1));
    }

    public void setRoundStatuses(List<String> roundStatuses) {
        if (roundStatuses == null || roundStatuses.isEmpty()) {
            this.roundStatusesSerialized = "";
            return;
        }

        this.roundStatusesSerialized = String.join("|", roundStatuses);
    }
}