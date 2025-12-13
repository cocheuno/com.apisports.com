package com.apisports.knime.football.model;

/**
 * Model class representing a football league.
 */
public class League {
    private final int id;
    private final String name;
    private final String country;
    private final String type;
    private final int season;
    
    public League(int id, String name, String country, String type, int season) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.type = type;
        this.season = season;
    }
    
    public int getId() { return id; }
    public String getName() { return name; }
    public String getCountry() { return country; }
    public String getType() { return type; }
    public int getSeason() { return season; }
}
