package com.apisports.knime.football.model;

/**
 * Model class representing player statistics.
 */
public class PlayerStats {
    private final String playerName;
    private final String team;
    private final int appearances;
    private final int goals;
    private final int assists;
    private final int yellowCards;
    private final int redCards;
    
    public PlayerStats(String playerName, String team, int appearances, int goals, 
                      int assists, int yellowCards, int redCards) {
        this.playerName = playerName;
        this.team = team;
        this.appearances = appearances;
        this.goals = goals;
        this.assists = assists;
        this.yellowCards = yellowCards;
        this.redCards = redCards;
    }
    
    public String getPlayerName() { return playerName; }
    public String getTeam() { return team; }
    public int getAppearances() { return appearances; }
    public int getGoals() { return goals; }
    public int getAssists() { return assists; }
    public int getYellowCards() { return yellowCards; }
    public int getRedCards() { return redCards; }
}
