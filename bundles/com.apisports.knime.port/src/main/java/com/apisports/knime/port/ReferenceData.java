package com.apisports.knime.port;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Container for reference data (countries, leagues, teams, venues).
 * Used to populate UI dropdowns in query nodes.
 */
public class ReferenceData implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Country> countries;
    private final List<League> leagues;
    private final List<Team> teams;
    private final List<Venue> venues;
    private final List<Season> seasons;
    private final long loadedTimestamp;

    public ReferenceData(List<Country> countries, List<League> leagues,
                        List<Team> teams, List<Venue> venues, List<Season> seasons) {
        this.countries = countries != null ? new ArrayList<>(countries) : new ArrayList<>();
        this.leagues = leagues != null ? new ArrayList<>(leagues) : new ArrayList<>();
        this.teams = teams != null ? new ArrayList<>(teams) : new ArrayList<>();
        this.venues = venues != null ? new ArrayList<>(venues) : new ArrayList<>();
        this.seasons = seasons != null ? new ArrayList<>(seasons) : new ArrayList<>();
        this.loadedTimestamp = System.currentTimeMillis();
    }

    // Backward compatibility constructor
    @Deprecated
    public ReferenceData(List<Country> countries, List<League> leagues,
                        List<Team> teams, List<Venue> venues) {
        this(countries, leagues, teams, venues, new ArrayList<>());
    }

    public List<Country> getCountries() {
        return new ArrayList<>(countries);
    }

    public List<League> getLeagues() {
        return new ArrayList<>(leagues);
    }

    public List<Team> getTeams() {
        return new ArrayList<>(teams);
    }

    public List<Venue> getVenues() {
        return new ArrayList<>(venues);
    }

    public List<Season> getSeasons() {
        return new ArrayList<>(seasons);
    }

    public long getLoadedTimestamp() {
        return loadedTimestamp;
    }

    /**
     * Get leagues for a specific country.
     */
    public List<League> getLeaguesForCountry(String countryName) {
        List<League> result = new ArrayList<>();
        for (League league : leagues) {
            if (countryName.equals(league.getCountryName())) {
                result.add(league);
            }
        }
        return result;
    }

    /**
     * Get teams for a specific league.
     */
    public List<Team> getTeamsForLeague(int leagueId) {
        List<Team> result = new ArrayList<>();
        for (Team team : teams) {
            if (team.getLeagueIds().contains(leagueId)) {
                result.add(team);
            }
        }
        return result;
    }

    /**
     * Get seasons for a specific league.
     */
    public List<Season> getSeasonsForLeague(int leagueId) {
        List<Season> result = new ArrayList<>();
        for (Season season : seasons) {
            if (season.getLeagueId() == leagueId) {
                result.add(season);
            }
        }
        return result;
    }

    // Simple data classes

    public static class Country implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final String code;
        private final String flag;

        public Country(String name, String code, String flag) {
            this.name = Objects.requireNonNull(name);
            this.code = code;
            this.flag = flag;
        }

        public String getName() { return name; }
        public String getCode() { return code; }
        public String getFlag() { return flag; }

        @Override
        public String toString() { return name; }
    }

    public static class League implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int id;
        private final String name;
        private final String type;
        private final String countryName;
        private final String logo;

        public League(int id, String name, String type, String countryName, String logo) {
            this.id = id;
            this.name = Objects.requireNonNull(name);
            this.type = type;
            this.countryName = countryName;
            this.logo = logo;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
        public String getCountryName() { return countryName; }
        public String getLogo() { return logo; }

        @Override
        public String toString() { return name; }
    }

    public static class Team implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int id;
        private final String name;
        private final String code;
        private final String country;
        private final String logo;
        private final List<Integer> leagueIds; // Teams can be in multiple leagues

        public Team(int id, String name, String code, String country, String logo, List<Integer> leagueIds) {
            this.id = id;
            this.name = Objects.requireNonNull(name);
            this.code = code;
            this.country = country;
            this.logo = logo;
            this.leagueIds = leagueIds != null ? new ArrayList<>(leagueIds) : new ArrayList<>();
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getCode() { return code; }
        public String getCountry() { return country; }
        public String getLogo() { return logo; }
        public List<Integer> getLeagueIds() { return new ArrayList<>(leagueIds); }

        @Override
        public String toString() { return name; }
    }

    public static class Venue implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int id;
        private final String name;
        private final String city;
        private final String country;
        private final Integer capacity;

        public Venue(int id, String name, String city, String country, Integer capacity) {
            this.id = id;
            this.name = Objects.requireNonNull(name);
            this.city = city;
            this.country = country;
            this.capacity = capacity;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getCity() { return city; }
        public String getCountry() { return country; }
        public Integer getCapacity() { return capacity; }

        @Override
        public String toString() { return name; }
    }

    public static class Season implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int leagueId;
        private final int year;
        private final String startDate;
        private final String endDate;
        private final boolean current;

        public Season(int leagueId, int year, String startDate, String endDate, boolean current) {
            this.leagueId = leagueId;
            this.year = year;
            this.startDate = startDate;
            this.endDate = endDate;
            this.current = current;
        }

        public int getLeagueId() { return leagueId; }
        public int getYear() { return year; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public boolean isCurrent() { return current; }

        @Override
        public String toString() { return String.valueOf(year); }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Season season = (Season) o;
            return leagueId == season.leagueId && year == season.year;
        }

        @Override
        public int hashCode() {
            return Objects.hash(leagueId, year);
        }
    }
}
