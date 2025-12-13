package com.apisports.knime.football.model;

import java.time.LocalDateTime;

/**
 * Model class representing a football fixture (match).
 */
public class Fixture {
    private final int id;
    private final String homeTeam;
    private final String awayTeam;
    private final LocalDateTime date;
    private final String status;
    private final Integer homeScore;
    private final Integer awayScore;
    
    public Fixture(int id, String homeTeam, String awayTeam, LocalDateTime date, 
                   String status, Integer homeScore, Integer awayScore) {
        this.id = id;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.date = date;
        this.status = status;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }
    
    public int getId() { return id; }
    public String getHomeTeam() { return homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public LocalDateTime getDate() { return date; }
    public String getStatus() { return status; }
    public Integer getHomeScore() { return homeScore; }
    public Integer getAwayScore() { return awayScore; }
}
