package com.apisports.knime.football.model;

/**
 * Model class representing a team's standing in a league.
 */
public class Standing {
    private final int rank;
    private final String team;
    private final int points;
    private final int played;
    private final int won;
    private final int drawn;
    private final int lost;
    private final int goalsFor;
    private final int goalsAgainst;
    private final int goalDifference;
    
    public Standing(int rank, String team, int points, int played, int won, int drawn, 
                   int lost, int goalsFor, int goalsAgainst, int goalDifference) {
        this.rank = rank;
        this.team = team;
        this.points = points;
        this.played = played;
        this.won = won;
        this.drawn = drawn;
        this.lost = lost;
        this.goalsFor = goalsFor;
        this.goalsAgainst = goalsAgainst;
        this.goalDifference = goalDifference;
    }
    
    public int getRank() { return rank; }
    public String getTeam() { return team; }
    public int getPoints() { return points; }
    public int getPlayed() { return played; }
    public int getWon() { return won; }
    public int getDrawn() { return drawn; }
    public int getLost() { return lost; }
    public int getGoalsFor() { return goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }
    public int getGoalDifference() { return goalDifference; }
}
