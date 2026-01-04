/*
 * Copyright 2025 Carone Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apisports.knime.football.nodes.referencedata;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.port.ApiSportsConnectionPortObject;
import com.apisports.knime.port.ReferenceDAO;
import com.apisports.knime.port.ReferenceData;
import com.apisports.knime.port.ReferenceData.Country;
import com.apisports.knime.port.ReferenceData.League;
import com.apisports.knime.port.ReferenceData.Season;
import com.apisports.knime.port.ReferenceData.Team;
import com.apisports.knime.port.ReferenceData.Venue;
import com.apisports.knime.port.ReferenceDataPortObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * NodeModel for Reference Data Loader.
 * Loads countries, leagues, teams, and venues from the API.
 */
public class ReferenceDataLoaderNodeModel extends NodeModel {

    /**
     * Helper class to hold leagues and seasons extracted from a single API response.
     */
    private static class LeaguesAndSeasons {
        final List<League> leagues;
        final List<Season> seasons;
        final Set<String> countries;

        LeaguesAndSeasons(List<League> leagues, List<Season> seasons, Set<String> countries) {
            this.leagues = leagues;
            this.seasons = seasons;
            this.countries = countries;
        }
    }

    static final String CFGKEY_LOAD_TEAMS = "loadTeams";
    static final String CFGKEY_LOAD_VENUES = "loadVenues";
    static final String CFGKEY_CACHE_TTL = "cacheTtl";
    static final String CFGKEY_COUNTRY_FILTER = "countryFilter";
    static final String CFGKEY_DB_PATH = "dbPath";
    static final String CFGKEY_CLEAR_AND_RELOAD = "clearAndReload";
    static final String CFGKEY_BEGIN_DATE = "beginDate";
    static final String CFGKEY_END_DATE = "endDate";
    static final String CFGKEY_SELECTED_SEASONS = "selectedSeasons";

    private final SettingsModelBoolean m_loadTeams =
        new SettingsModelBoolean(CFGKEY_LOAD_TEAMS, true);
    private final SettingsModelBoolean m_loadVenues =
        new SettingsModelBoolean(CFGKEY_LOAD_VENUES, false);
    private final SettingsModelInteger m_cacheTtl =
        new SettingsModelInteger(CFGKEY_CACHE_TTL, 86400); // 24 hours default
    private final SettingsModelStringArray m_countryFilter =
        new SettingsModelStringArray(CFGKEY_COUNTRY_FILTER, new String[0]);
    private final SettingsModelString m_dbPath =
        new SettingsModelString(CFGKEY_DB_PATH, getDefaultDbPath());
    private final SettingsModelBoolean m_clearAndReload =
        new SettingsModelBoolean(CFGKEY_CLEAR_AND_RELOAD, false);
    private final SettingsModelString m_beginDate =
        new SettingsModelString(CFGKEY_BEGIN_DATE, "");
    private final SettingsModelString m_endDate =
        new SettingsModelString(CFGKEY_END_DATE, "");
    private final SettingsModelStringArray m_selectedSeasons =
        new SettingsModelStringArray(CFGKEY_SELECTED_SEASONS, new String[0]);

    public static String getDefaultDbPath() {
        String userHome = System.getProperty("user.home");
        return userHome + File.separator + ".apisports" + File.separator + "football_ref.db";
    }

    protected ReferenceDataLoaderNodeModel() {
        super(new PortType[]{ApiSportsConnectionPortObject.TYPE},
              new PortType[]{ReferenceDataPortObject.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        // Get API client
        ApiSportsConnectionPortObject connectionPort = (ApiSportsConnectionPortObject) inObjects[0];
        ApiSportsHttpClient client = connectionPort.getClient();
        ObjectMapper mapper = new ObjectMapper();

        // Initialize SQLite database
        String dbPath = m_dbPath.getStringValue();
        exec.setMessage("Initializing database at " + dbPath + "...");
        exec.setProgress(0.05);

        try (ReferenceDAO dao = new ReferenceDAO(dbPath)) {
            // Generate configuration hash based on current settings
            String currentConfigHash = generateConfigurationHash();

            // Check if data is fresh based on cache TTL and configuration
            int cacheTtl = m_cacheTtl.getIntValue();
            boolean dataStale = dao.isDataStale(cacheTtl);
            boolean hasData = dao.hasData();
            boolean configChanged = dao.hasConfigurationChanged(currentConfigHash);

            // If "Clear and Reload" is checked, always reload
            if (m_clearAndReload.getBooleanValue()) {
                exec.setMessage("Clearing existing data (Clear and Reload selected)...");
                dao.clearAll();
                getLogger().info("Database cleared - will reload all data");
                dataStale = true; // Force reload
            }
            // If configuration changed, reload even if data is fresh
            else if (hasData && configChanged) {
                exec.setMessage("Configuration changed - reloading...");
                getLogger().info("Configuration has changed - reloading reference data");
                dao.clearLeaguesAndRelatedData();
            }
            // If data exists and is still fresh, skip loading
            else if (hasData && !dataStale) {
                long lastUpdate = dao.getLastUpdateTimestamp();
                long ageSeconds = (System.currentTimeMillis() - lastUpdate) / 1000;
                exec.setMessage("Using cached data (age: " + ageSeconds + "s, TTL: " + cacheTtl + "s)");
                getLogger().info("Reference data is still fresh (age: " + ageSeconds + " seconds, TTL: " +
                               cacheTtl + " seconds) - skipping reload");

                // Return existing database reference without reloading
                ReferenceDataPortObject output = new ReferenceDataPortObject(dbPath);
                exec.setProgress(1.0);
                return new PortObject[]{output};
            }
            // Data is stale or doesn't exist - reload
            else {
                if (hasData) {
                    long lastUpdate = dao.getLastUpdateTimestamp();
                    long ageSeconds = (System.currentTimeMillis() - lastUpdate) / 1000;
                    exec.setMessage("Data is stale (age: " + ageSeconds + "s, TTL: " + cacheTtl + "s) - reloading...");
                    getLogger().info("Reference data is stale (age: " + ageSeconds + " seconds, TTL: " +
                                   cacheTtl + " seconds) - reloading");
                } else {
                    exec.setMessage("No cached data found - loading...");
                    getLogger().info("No cached data found - performing initial load");
                }
                // Clear leagues/seasons/teams to ensure database matches current filters
                // (Countries remain since they're always loaded)
                dao.clearLeaguesAndRelatedData();
            }

            // Get country filter
            Set<String> countryFilter = new HashSet<>(Arrays.asList(m_countryFilter.getStringArrayValue()));
            boolean hasCountryFilter = !countryFilter.isEmpty();

            // Load leagues and seasons in a single optimized call
            // The /leagues endpoint returns seasons embedded in the response, so we extract both
            exec.setMessage("Loading leagues and seasons...");
            exec.setProgress(0.2);
            LeaguesAndSeasons data = loadLeaguesWithSeasons(client, mapper, countryFilter, hasCountryFilter);

            getLogger().info("Loaded " + data.leagues.size() + " leagues and " +
                           data.seasons.size() + " seasons" +
                           (hasCountryFilter ? " for countries: " + String.join(", ", countryFilter) : " (all countries)"));

            // Store countries extracted from league data
            List<Country> countries = new ArrayList<>();
            for (String countryName : data.countries) {
                countries.add(new Country(countryName, null, null));
            }
            dao.upsertCountries(countries);
            getLogger().info("Extracted " + countries.size() + " countries from league data");

            // Store leagues
            dao.upsertLeagues(data.leagues);

            // Filter seasons by date range or selected seasons
            exec.setMessage("Filtering seasons...");
            exec.setProgress(0.4);
            List<Season> filteredSeasons = filterSeasonsByDateOrSelection(data.seasons);
            dao.upsertSeasons(filteredSeasons);
            getLogger().info("Stored " + filteredSeasons.size() + " seasons (filtered from " +
                            data.seasons.size() + " total)");

            // Load teams if enabled
            if (m_loadTeams.getBooleanValue()) {
                exec.setMessage("Loading teams...");
                exec.setProgress(0.6);
                List<Team> teams = loadTeams(client, mapper, data.leagues);
                dao.upsertTeams(teams);
                getLogger().info("Loaded " + teams.size() + " teams");
            }

            // Load venues if enabled (not yet implemented)
            if (m_loadVenues.getBooleanValue()) {
                exec.setMessage("Loading venues...");
                exec.setProgress(0.8);
                getLogger().warn("Venue loading not yet implemented");
            }

            exec.setMessage("Finalizing...");
            exec.setProgress(0.95);

            // Update timestamp and configuration hash to mark data as fresh
            dao.setLastUpdateTimestamp(System.currentTimeMillis());
            dao.setConfigurationHash(currentConfigHash);
            getLogger().info("Updated cache timestamp - data will remain fresh for " + cacheTtl + " seconds");

            // Create port object with DB path
            ReferenceDataPortObject output = new ReferenceDataPortObject(dbPath);

            exec.setProgress(1.0);
            getLogger().info("Reference data successfully saved to " + dbPath);

            return new PortObject[]{output};
        }
    }

    /**
     * Filter seasons based on date range or selected seasons.
     * Logic: If begin date is set, use date range. Otherwise, use selected seasons.
     */
    private List<Season> filterSeasonsByDateOrSelection(List<Season> allSeasons) throws Exception {
        String beginDateStr = m_beginDate.getStringValue();
        String endDateStr = m_endDate.getStringValue();
        String[] selectedSeasons = m_selectedSeasons.getStringArrayValue();

        // If begin date is set (not empty), filter by date range
        if (beginDateStr != null && !beginDateStr.trim().isEmpty()) {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
            java.util.Date beginDate = dateFormat.parse(beginDateStr);
            java.util.Date endDate = null;

            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                endDate = dateFormat.parse(endDateStr);
            }

            getLogger().info("Filtering seasons by date range: " + beginDateStr +
                           (endDate != null ? " to " + endDateStr : " onwards"));

            List<Season> filtered = new ArrayList<>();

            for (Season season : allSeasons) {
                try {
                    // Parse season start date
                    if (season.getStartDate() != null) {
                        java.util.Date seasonStart = dateFormat.parse(season.getStartDate());

                        // Check if season starts after or on begin date
                        if (!seasonStart.before(beginDate)) {
                            // If end date is set, check if season starts before or on end date
                            if (endDate == null || !seasonStart.after(endDate)) {
                                filtered.add(season);
                            }
                        }
                    }
                } catch (java.text.ParseException e) {
                    getLogger().warn("Could not parse season start date: " + season.getStartDate());
                }
            }

            return filtered;
        }
        // If seasons are selected, filter by year
        else if (selectedSeasons.length > 0) {
            Set<Integer> selectedYears = new HashSet<>();
            for (String year : selectedSeasons) {
                try {
                    selectedYears.add(Integer.parseInt(year));
                } catch (NumberFormatException e) {
                    getLogger().warn("Invalid season year: " + year);
                }
            }

            getLogger().info("Filtering seasons by years: " + selectedYears);

            List<Season> filtered = new ArrayList<>();
            for (Season season : allSeasons) {
                if (selectedYears.contains(season.getYear())) {
                    filtered.add(season);
                }
            }

            return filtered;
        }
        // No filter - return all seasons
        else {
            getLogger().info("No season filter applied - loading all seasons from 2008+");
            return allSeasons;
        }
    }

    /**
     * OPTIMIZED: Load leagues and seasons from a single /leagues API call.
     * The API returns seasons embedded in the response, so we extract both together.
     * This eliminates redundant API calls and dramatically improves performance.
     *
     * For England + 2024: Previously ~22 API calls, now just 1 call for leagues/seasons!
     */
    private LeaguesAndSeasons loadLeaguesWithSeasons(ApiSportsHttpClient client, ObjectMapper mapper,
                                                      Set<String> countryFilter, boolean hasCountryFilter) throws Exception {
        List<League> leagues = new ArrayList<>();
        List<Season> seasons = new ArrayList<>();
        Set<String> countries = new HashSet<>();

        Map<String, String> params = new HashMap<>();

        // If country filter is specified, use it to reduce data transfer
        if (hasCountryFilter && countryFilter.size() == 1) {
            // Single country - use filtered request
            String country = countryFilter.iterator().next();
            params.put("country", country);
            getLogger().info("Making API call: GET /leagues?country=" + country);
        } else if (hasCountryFilter && countryFilter.size() > 1) {
            // Multiple countries - need multiple calls
            for (String country : countryFilter) {
                params.clear();
                params.put("country", country);
                getLogger().info("Making API call: GET /leagues?country=" + country);
                LeaguesAndSeasons partial = parseLeaguesResponse(client.get("/leagues", params), mapper);
                leagues.addAll(partial.leagues);
                seasons.addAll(partial.seasons);
                countries.addAll(partial.countries);
            }
            return new LeaguesAndSeasons(leagues, seasons, countries);
        } else {
            // No filter - load all leagues
            getLogger().info("Making API call: GET /leagues (all countries)");
        }

        // Make the API call and parse response
        String response = client.get("/leagues", params);
        return parseLeaguesResponse(response, mapper);
    }

    /**
     * Parse /leagues API response and extract leagues, seasons, and countries.
     * Seasons are embedded in each league object, so we extract them together.
     */
    private LeaguesAndSeasons parseLeaguesResponse(String response, ObjectMapper mapper) throws Exception {
        List<League> leagues = new ArrayList<>();
        List<Season> seasons = new ArrayList<>();
        Set<String> countries = new HashSet<>();

        JsonNode root = mapper.readTree(response);
        JsonNode responseArray = root.get("response");

        if (responseArray != null && responseArray.isArray()) {
            for (JsonNode item : responseArray) {
                JsonNode leagueNode = item.get("league");
                JsonNode countryNode = item.get("country");
                JsonNode seasonsArray = item.get("seasons");

                if (leagueNode != null) {
                    int id = leagueNode.has("id") ? leagueNode.get("id").asInt() : 0;
                    String name = leagueNode.has("name") ? leagueNode.get("name").asText() : "";
                    String type = leagueNode.has("type") ? leagueNode.get("type").asText() : "";
                    String logo = leagueNode.has("logo") ? leagueNode.get("logo").asText() : null;

                    String countryName = "";
                    if (countryNode != null && countryNode.has("name")) {
                        countryName = countryNode.get("name").asText();
                        countries.add(countryName);
                    }

                    if (id > 0 && !name.isEmpty()) {
                        leagues.add(new League(id, name, type, countryName, logo));

                        // Extract seasons for this league (from the same API response!)
                        if (seasonsArray != null && seasonsArray.isArray()) {
                            for (JsonNode seasonNode : seasonsArray) {
                                int year = seasonNode.has("year") ? seasonNode.get("year").asInt() : 0;
                                String start = seasonNode.has("start") ? seasonNode.get("start").asText() : null;
                                String end = seasonNode.has("end") ? seasonNode.get("end").asText() : null;
                                boolean current = seasonNode.has("current") && seasonNode.get("current").asBoolean();

                                if (year >= 2008) { // Filter seasons from 2008 onwards
                                    seasons.add(new Season(id, year, start, end, current));
                                }
                            }
                        }
                    }
                }
            }
        }

        return new LeaguesAndSeasons(leagues, seasons, countries);
    }

    /**
     * Load teams from /teams endpoint for each league.
     * Loads teams for all filtered leagues using the most recent season.
     * Note: Unfortunately the API requires one call per league for teams.
     */
    private List<Team> loadTeams(ApiSportsHttpClient client, ObjectMapper mapper,
                                  List<League> leagues) throws Exception {
        List<Team> teams = new ArrayList<>();
        Map<Integer, Team> teamMap = new HashMap<>(); // Deduplicate teams by ID

        // Determine which season to use for team queries
        // IMPORTANT: Don't use future seasons (like 2026 in January 2026) as they have no data
        // Default to previous year which is guaranteed to have complete data
        int currentYear = java.time.Year.now().getValue();
        int seasonToUse = currentYear - 1;  // Use previous year as default (has complete data)

        String[] selectedSeasons = m_selectedSeasons.getStringArrayValue();
        if (selectedSeasons.length > 0) {
            // Use the first selected season (could be from UI selection)
            try {
                int selectedYear = Integer.parseInt(selectedSeasons[0]);
                // Only use selected year if it's not in the future
                if (selectedYear <= currentYear) {
                    seasonToUse = selectedYear;
                } else {
                    getLogger().warn("Selected season " + selectedYear + " is in the future, using " + seasonToUse + " instead");
                }
            } catch (NumberFormatException e) {
                getLogger().warn("Invalid season format, using default: " + seasonToUse);
            }
        }

        getLogger().info("Loading teams for " + leagues.size() + " leagues using season " + seasonToUse);

        // Load teams for ALL filtered leagues
        for (int i = 0; i < leagues.size(); i++) {
            League league = leagues.get(i);

            Map<String, String> params = new HashMap<>();
            params.put("league", String.valueOf(league.getId()));
            params.put("season", String.valueOf(seasonToUse));

            try {
                String response = client.get("/teams", params);
                JsonNode root = mapper.readTree(response);
                JsonNode responseArray = root.get("response");

                if (responseArray != null && responseArray.isArray()) {
                    for (JsonNode item : responseArray) {
                        JsonNode teamNode = item.get("team");

                        if (teamNode != null) {
                            int id = teamNode.has("id") ? teamNode.get("id").asInt() : 0;
                            String name = teamNode.has("name") ? teamNode.get("name").asText() : "";
                            String code = teamNode.has("code") ? teamNode.get("code").asText() : null;
                            String country = teamNode.has("country") ? teamNode.get("country").asText() : "";
                            String logo = teamNode.has("logo") ? teamNode.get("logo").asText() : null;

                            if (id > 0 && !name.isEmpty()) {
                                // Add or update team with league ID
                                if (teamMap.containsKey(id)) {
                                    Team existingTeam = teamMap.get(id);
                                    List<Integer> leagueIds = new ArrayList<>(existingTeam.getLeagueIds());
                                    if (!leagueIds.contains(league.getId())) {
                                        leagueIds.add(league.getId());
                                        teamMap.put(id, new Team(id, name, code, country, logo, leagueIds));
                                    }
                                } else {
                                    List<Integer> leagueIds = new ArrayList<>();
                                    leagueIds.add(league.getId());
                                    teamMap.put(id, new Team(id, name, code, country, logo, leagueIds));
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                getLogger().warn("Failed to load teams for league " + league.getName() + ": " + e.getMessage());
            }

            // Log progress
            if (i % 5 == 0 || i == leagues.size() - 1) {
                getLogger().info("Loaded teams for " + (i + 1) + "/" + leagues.size() + " leagues");
            }
        }

        teams.addAll(teamMap.values());
        return teams;
    }

    /**
     * Load venues from /venues endpoint.
     * Note: This endpoint may not be available in all API tiers.
     */
    private List<Venue> loadVenues(ApiSportsHttpClient client, ObjectMapper mapper) throws Exception {
        List<Venue> venues = new ArrayList<>();
        // TODO: Implement venue loading if needed
        // The /venues endpoint requires country or team parameter
        getLogger().warn("Venue loading not yet implemented");
        return venues;
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // Cannot determine output spec without execution
        return new PortObjectSpec[]{null};
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_loadTeams.saveSettingsTo(settings);
        m_loadVenues.saveSettingsTo(settings);
        m_cacheTtl.saveSettingsTo(settings);
        m_countryFilter.saveSettingsTo(settings);
        m_dbPath.saveSettingsTo(settings);
        m_clearAndReload.saveSettingsTo(settings);
        m_beginDate.saveSettingsTo(settings);
        m_endDate.saveSettingsTo(settings);
        m_selectedSeasons.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_loadTeams.validateSettings(settings);
        m_loadVenues.validateSettings(settings);
        m_cacheTtl.validateSettings(settings);
        m_countryFilter.validateSettings(settings);
        m_dbPath.validateSettings(settings);
        m_clearAndReload.validateSettings(settings);
        // New settings - backwards compatible (don't validate if missing)
        if (settings.containsKey(CFGKEY_BEGIN_DATE)) {
            m_beginDate.validateSettings(settings);
        }
        if (settings.containsKey(CFGKEY_END_DATE)) {
            m_endDate.validateSettings(settings);
        }
        if (settings.containsKey(CFGKEY_SELECTED_SEASONS)) {
            m_selectedSeasons.validateSettings(settings);
        }
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_loadTeams.loadSettingsFrom(settings);
        m_loadVenues.loadSettingsFrom(settings);
        m_cacheTtl.loadSettingsFrom(settings);
        m_countryFilter.loadSettingsFrom(settings);
        m_dbPath.loadSettingsFrom(settings);
        m_clearAndReload.loadSettingsFrom(settings);
        // New settings - backwards compatible
        try {
            m_beginDate.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            // Use default (null)
        }
        try {
            m_endDate.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            // Use default (null)
        }
        try {
            m_selectedSeasons.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            // Use default (empty array)
        }
    }

    @Override
    protected void reset() {
        // Nothing to reset
    }

    @Override
    protected void loadInternals(final java.io.File nodeInternDir,
                                 final org.knime.core.node.ExecutionMonitor exec)
            throws java.io.IOException, org.knime.core.node.CanceledExecutionException {
        // No internal state to load
    }

    @Override
    protected void saveInternals(final java.io.File nodeInternDir,
                                 final org.knime.core.node.ExecutionMonitor exec)
            throws java.io.IOException, org.knime.core.node.CanceledExecutionException {
        // No internal state to save
    }

    /**
     * Generate a hash of the current configuration to detect changes.
     * Includes: country filter, selected seasons, begin/end dates, load teams flag
     */
    private String generateConfigurationHash() {
        StringBuilder config = new StringBuilder();

        // Country filter
        String[] countries = m_countryFilter.getStringArrayValue();
        java.util.Arrays.sort(countries); // Sort for consistent hashing
        config.append("countries:").append(String.join(",", countries)).append(";");

        // Selected seasons
        String[] seasons = m_selectedSeasons.getStringArrayValue();
        java.util.Arrays.sort(seasons); // Sort for consistent hashing
        config.append("seasons:").append(String.join(",", seasons)).append(";");

        // Date range
        config.append("beginDate:").append(m_beginDate.getStringValue()).append(";");
        config.append("endDate:").append(m_endDate.getStringValue()).append(";");

        // Load teams flag
        config.append("loadTeams:").append(m_loadTeams.getBooleanValue());

        // Return hash of configuration string
        return String.valueOf(config.toString().hashCode());
    }
}
