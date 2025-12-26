package com.apisports.knime.port;

import com.apisports.knime.port.ReferenceData.Country;
import com.apisports.knime.port.ReferenceData.League;
import com.apisports.knime.port.ReferenceData.Season;
import com.apisports.knime.port.ReferenceData.Team;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Reference Data SQLite database.
 * Handles all CRUD operations for countries, leagues, seasons, and teams.
 * Uses prepared statements to prevent SQL injection.
 */
public class ReferenceDAO implements AutoCloseable {

    private final String dbPath;
    private Connection connection;

    public ReferenceDAO(String dbPath) throws SQLException {
        this.dbPath = dbPath;
        // Ensure parent directory exists
        File dbFile = new File(dbPath);
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Explicitly load SQLite JDBC driver (required in OSGi environments)
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found. Ensure sqlite-jdbc JAR is in bundle classpath.", e);
        }

        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        initializeDatabase();
    }

    /**
     * Initialize database schema if tables don't exist.
     */
    private void initializeDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Countries table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS countries (" +
                "  name TEXT PRIMARY KEY," +
                "  code TEXT," +
                "  flag TEXT" +
                ")"
            );

            // Leagues table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS leagues (" +
                "  id INTEGER PRIMARY KEY," +
                "  name TEXT NOT NULL," +
                "  type TEXT," +
                "  country_name TEXT," +
                "  logo TEXT" +
                ")"
            );

            // Seasons table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS seasons (" +
                "  league_id INTEGER," +
                "  year INTEGER," +
                "  start_date TEXT," +
                "  end_date TEXT," +
                "  is_current INTEGER," +
                "  PRIMARY KEY(league_id, year)" +
                ")"
            );

            // Teams table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS teams (" +
                "  id INTEGER PRIMARY KEY," +
                "  name TEXT NOT NULL," +
                "  code TEXT," +
                "  country TEXT," +
                "  logo TEXT" +
                ")"
            );

            // Team-League junction table (many-to-many)
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS team_leagues (" +
                "  team_id INTEGER," +
                "  league_id INTEGER," +
                "  PRIMARY KEY(team_id, league_id)" +
                ")"
            );

            // Metadata table for tracking cache timestamps
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS metadata (" +
                "  key TEXT PRIMARY KEY," +
                "  value TEXT" +
                ")"
            );

            // Create indices for faster queries
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_leagues_country ON leagues(country_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_seasons_league ON seasons(league_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_team_leagues_league ON team_leagues(league_id)");
        }
    }

    /**
     * Clear all data from all tables.
     */
    public void clearAll() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM team_leagues");
            stmt.execute("DELETE FROM teams");
            stmt.execute("DELETE FROM seasons");
            stmt.execute("DELETE FROM leagues");
            stmt.execute("DELETE FROM countries");
            stmt.execute("DELETE FROM metadata");
        }
    }

    /**
     * Clear leagues, seasons, and teams (but keep countries).
     * Use this when reloading with different filters.
     */
    public void clearLeaguesAndRelatedData() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM team_leagues");
            stmt.execute("DELETE FROM teams");
            stmt.execute("DELETE FROM seasons");
            stmt.execute("DELETE FROM leagues");
        }
    }

    // ========== Metadata Operations ==========

    /**
     * Get the timestamp when reference data was last updated.
     * @return Timestamp in milliseconds, or 0 if never updated
     */
    public long getLastUpdateTimestamp() throws SQLException {
        String sql = "SELECT value FROM metadata WHERE key = 'last_update_timestamp'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return Long.parseLong(rs.getString("value"));
            }
        } catch (NumberFormatException e) {
            return 0;
        }
        return 0;
    }

    /**
     * Set the timestamp when reference data was last updated.
     * @param timestamp Timestamp in milliseconds
     */
    public void setLastUpdateTimestamp(long timestamp) throws SQLException {
        String sql = "INSERT OR REPLACE INTO metadata (key, value) VALUES ('last_update_timestamp', ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, String.valueOf(timestamp));
            pstmt.executeUpdate();
        }
    }

    /**
     * Check if reference data is stale based on TTL.
     * @param ttlSeconds Time-to-live in seconds
     * @return true if data is stale and needs refresh, false if still fresh
     */
    public boolean isDataStale(int ttlSeconds) throws SQLException {
        long lastUpdate = getLastUpdateTimestamp();
        if (lastUpdate == 0) {
            return true; // No data loaded yet
        }
        long currentTime = System.currentTimeMillis();
        long ageSeconds = (currentTime - lastUpdate) / 1000;
        return ageSeconds >= ttlSeconds;
    }

    /**
     * Check if the database has any data loaded.
     * @return true if leagues exist, false otherwise
     */
    public boolean hasData() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM leagues";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        }
        return false;
    }

    // ========== Country Operations ==========

    public void upsertCountries(List<Country> countries) throws SQLException {
        String sql = "INSERT OR REPLACE INTO countries (name, code, flag) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (Country country : countries) {
                pstmt.setString(1, country.getName());
                pstmt.setString(2, country.getCode());
                pstmt.setString(3, country.getFlag());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
        }
    }

    public List<Country> getAllCountries() throws SQLException {
        List<Country> countries = new ArrayList<>();
        String sql = "SELECT name, code, flag FROM countries ORDER BY name";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                countries.add(new Country(
                    rs.getString("name"),
                    rs.getString("code"),
                    rs.getString("flag")
                ));
            }
        }
        return countries;
    }

    // ========== League Operations ==========

    public void upsertLeagues(List<League> leagues) throws SQLException {
        String sql = "INSERT OR REPLACE INTO leagues (id, name, type, country_name, logo) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (League league : leagues) {
                pstmt.setInt(1, league.getId());
                pstmt.setString(2, league.getName());
                pstmt.setString(3, league.getType());
                pstmt.setString(4, league.getCountryName());
                pstmt.setString(5, league.getLogo());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
        }
    }

    public List<League> getAllLeagues() throws SQLException {
        List<League> leagues = new ArrayList<>();
        String sql = "SELECT id, name, type, country_name, logo FROM leagues ORDER BY name";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                leagues.add(new League(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getString("country_name"),
                    rs.getString("logo")
                ));
            }
        }
        return leagues;
    }

    public List<League> getLeaguesByCountry(String countryName) throws SQLException {
        List<League> leagues = new ArrayList<>();
        String sql = "SELECT id, name, type, country_name, logo FROM leagues WHERE country_name = ? ORDER BY name";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, countryName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    leagues.add(new League(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("country_name"),
                        rs.getString("logo")
                    ));
                }
            }
        }
        return leagues;
    }

    // ========== Season Operations ==========

    public void upsertSeasons(List<Season> seasons) throws SQLException {
        String sql = "INSERT OR REPLACE INTO seasons (league_id, year, start_date, end_date, is_current) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (Season season : seasons) {
                pstmt.setInt(1, season.getLeagueId());
                pstmt.setInt(2, season.getYear());
                pstmt.setString(3, season.getStartDate());
                pstmt.setString(4, season.getEndDate());
                pstmt.setInt(5, season.isCurrent() ? 1 : 0);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
        }
    }

    public List<Season> getAllSeasons() throws SQLException {
        List<Season> seasons = new ArrayList<>();
        String sql = "SELECT league_id, year, start_date, end_date, is_current FROM seasons ORDER BY year DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                seasons.add(new Season(
                    rs.getInt("league_id"),
                    rs.getInt("year"),
                    rs.getString("start_date"),
                    rs.getString("end_date"),
                    rs.getInt("is_current") == 1
                ));
            }
        }
        return seasons;
    }

    public List<Season> getSeasonsByLeague(int leagueId) throws SQLException {
        List<Season> seasons = new ArrayList<>();
        String sql = "SELECT league_id, year, start_date, end_date, is_current " +
                     "FROM seasons WHERE league_id = ? ORDER BY year DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, leagueId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    seasons.add(new Season(
                        rs.getInt("league_id"),
                        rs.getInt("year"),
                        rs.getString("start_date"),
                        rs.getString("end_date"),
                        rs.getInt("is_current") == 1
                    ));
                }
            }
        }
        return seasons;
    }

    // ========== Team Operations ==========

    public void upsertTeams(List<Team> teams) throws SQLException {
        String teamSql = "INSERT OR REPLACE INTO teams (id, name, code, country, logo) VALUES (?, ?, ?, ?, ?)";
        String junctionSql = "INSERT OR IGNORE INTO team_leagues (team_id, league_id) VALUES (?, ?)";

        connection.setAutoCommit(false);

        try (PreparedStatement teamStmt = connection.prepareStatement(teamSql);
             PreparedStatement junctionStmt = connection.prepareStatement(junctionSql)) {

            for (Team team : teams) {
                // Insert team
                teamStmt.setInt(1, team.getId());
                teamStmt.setString(2, team.getName());
                teamStmt.setString(3, team.getCode());
                teamStmt.setString(4, team.getCountry());
                teamStmt.setString(5, team.getLogo());
                teamStmt.addBatch();

                // Insert team-league associations
                for (int leagueId : team.getLeagueIds()) {
                    junctionStmt.setInt(1, team.getId());
                    junctionStmt.setInt(2, leagueId);
                    junctionStmt.addBatch();
                }
            }

            teamStmt.executeBatch();
            junctionStmt.executeBatch();
            connection.commit();
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public List<Team> getAllTeams() throws SQLException {
        Map<Integer, Team> teamMap = new HashMap<>();

        String sql = "SELECT t.id, t.name, t.code, t.country, t.logo, tl.league_id " +
                     "FROM teams t " +
                     "LEFT JOIN team_leagues tl ON t.id = tl.team_id " +
                     "ORDER BY t.name";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int teamId = rs.getInt("id");

                if (!teamMap.containsKey(teamId)) {
                    teamMap.put(teamId, new Team(
                        teamId,
                        rs.getString("name"),
                        rs.getString("code"),
                        rs.getString("country"),
                        rs.getString("logo"),
                        new ArrayList<>()
                    ));
                }

                // Add league ID if present
                int leagueId = rs.getInt("league_id");
                if (!rs.wasNull() && leagueId > 0) {
                    Team existingTeam = teamMap.get(teamId);
                    List<Integer> leagueIds = new ArrayList<>(existingTeam.getLeagueIds());
                    if (!leagueIds.contains(leagueId)) {
                        leagueIds.add(leagueId);
                        teamMap.put(teamId, new Team(
                            existingTeam.getId(),
                            existingTeam.getName(),
                            existingTeam.getCode(),
                            existingTeam.getCountry(),
                            existingTeam.getLogo(),
                            leagueIds
                        ));
                    }
                }
            }
        }

        return new ArrayList<>(teamMap.values());
    }

    public List<Team> getTeamsByLeague(int leagueId) throws SQLException {
        Map<Integer, Team> teamMap = new HashMap<>();

        String sql = "SELECT t.id, t.name, t.code, t.country, t.logo, tl.league_id " +
                     "FROM teams t " +
                     "JOIN team_leagues tl ON t.id = tl.team_id " +
                     "WHERE tl.league_id = ? " +
                     "ORDER BY t.name";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, leagueId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int teamId = rs.getInt("id");

                    if (!teamMap.containsKey(teamId)) {
                        List<Integer> leagueIds = new ArrayList<>();
                        leagueIds.add(leagueId);

                        teamMap.put(teamId, new Team(
                            teamId,
                            rs.getString("name"),
                            rs.getString("code"),
                            rs.getString("country"),
                            rs.getString("logo"),
                            leagueIds
                        ));
                    }
                }
            }
        }

        return new ArrayList<>(teamMap.values());
    }

    // ========== Utility Methods ==========

    /**
     * Get database file path.
     */
    public String getDbPath() {
        return dbPath;
    }

    /**
     * Check if database is empty.
     */
    public boolean isEmpty() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM leagues";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("count") == 0;
            }
        }
        return true;
    }

    /**
     * Close the database connection.
     */
    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
