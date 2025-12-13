package com.apisports.knime.football.model;

/**
 * Model class representing a football fixture event (goal, card, etc).
 */
public class FixtureEvent {
    private final int fixtureId;
    private final int minute;
    private final String type;
    private final String team;
    private final String player;
    private final String detail;
    
    public FixtureEvent(int fixtureId, int minute, String type, String team, 
                       String player, String detail) {
        this.fixtureId = fixtureId;
        this.minute = minute;
        this.type = type;
        this.team = team;
        this.player = player;
        this.detail = detail;
    }
    
    public int getFixtureId() { return fixtureId; }
    public int getMinute() { return minute; }
    public String getType() { return type; }
    public String getTeam() { return team; }
    public String getPlayer() { return player; }
    public String getDetail() { return detail; }
}
