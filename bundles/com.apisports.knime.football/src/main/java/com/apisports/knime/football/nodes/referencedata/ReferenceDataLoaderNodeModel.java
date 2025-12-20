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

    // Rate limiting: 10 requests per minute = 6 seconds between requests
    private static final long RATE_LIMIT_DELAY_MS = 6000;
    private long lastApiCallTime = 0;

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

    /**
     * Enforce rate limit of 10 requests per minute (6 seconds between calls).
     * Displays progress message while waiting.
     */
    private void waitForRateLimit(ExecutionContext exec, String message) throws Exception {
        long now = System.currentTimeMillis();
        long timeSinceLastCall = now - lastApiCallTime;

        if (lastApiCallTime > 0 && timeSinceLastCall < RATE_LIMIT_DELAY_MS) {
            long waitTime = RATE_LIMIT_DELAY_MS - timeSinceLastCall;
            exec.setMessage(message + " (rate limit: waiting " + (waitTime / 1000) + "s...)");
            Thread.sleep(waitTime);
        }

        lastApiCallTime = System.currentTimeMillis();
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
            // Clear database if requested
            if (m_clearAndReload.getBooleanValue()) {
                exec.setMessage("Clearing existing data...");
                dao.clearAll();
                getLogger().info("Database cleared");
            } else {
                // Always clear leagues/seasons/teams to ensure database matches current filters
                // (Countries remain since they're always loaded)
                exec.setMessage("Clearing previous leagues/seasons/teams...");
                dao.clearLeaguesAndRelatedData();
                getLogger().info("Cleared previous leagues/seasons/teams");
            }

            // Get country filter
            Set<String> countryFilter = new HashSet<>(Arrays.asList(m_countryFilter.getStringArrayValue()));
            boolean hasCountryFilter = !countryFilter.isEmpty();

            // Load countries
            exec.setMessage("Loading countries...");
            exec.setProgress(0.1);
            List<Country> countries = loadCountries(client, mapper, exec);
            dao.upsertCountries(countries);
            getLogger().info("Loaded " + countries.size() + " countries");

            // Load leagues (filtered by country if specified)
            exec.setMessage("Loading leagues...");
            exec.setProgress(0.2);
            List<League> leaguesToStore;
            if (hasCountryFilter) {
                leaguesToStore = loadLeaguesFiltered(client, mapper, countryFilter, exec);
                getLogger().info("Loaded " + leaguesToStore.size() + " leagues for countries: " +
                                String.join(", ", countryFilter));
            } else {
                leaguesToStore = loadLeagues(client, mapper, exec);
                getLogger().info("Loaded " + leaguesToStore.size() + " leagues (all countries)");
            }


            dao.upsertLeagues(leaguesToStore);

            // Load seasons for filtered leagues
            exec.setMessage("Loading seasons...");
            exec.setProgress(0.4);
            List<Season> allSeasons = loadSeasons(client, mapper, leaguesToStore, exec);

            // Filter seasons by date range or selected seasons
            List<Season> filteredSeasons = filterSeasonsByDateOrSelection(allSeasons);
            dao.upsertSeasons(filteredSeasons);
            getLogger().info("Loaded " + filteredSeasons.size() + " seasons (filtered from " +
                            allSeasons.size() + " total)");

            // Load teams if enabled
            if (m_loadTeams.getBooleanValue()) {
                exec.setMessage("Loading teams...");
                exec.setProgress(0.6);
                List<Team> teams = loadTeams(client, mapper, leaguesToStore, exec);
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
     * Load countries from /countries endpoint.
     */
    private List<Country> loadCountries(ApiSportsHttpClient client, ObjectMapper mapper, ExecutionContext exec) throws Exception {
        List<Country> countries = new ArrayList<>();
        Map<String, String> params = new HashMap<>();

        waitForRateLimit(exec, "Loading countries");
        String response = client.get("/countries", params);
        JsonNode root = mapper.readTree(response);
        JsonNode responseArray = root.get("response");

        if (responseArray != null && responseArray.isArray()) {
            for (JsonNode countryNode : responseArray) {
                String name = countryNode.has("name") ? countryNode.get("name").asText() : "";
                String code = countryNode.has("code") ? countryNode.get("code").asText() : null;
                String flag = countryNode.has("flag") ? countryNode.get("flag").asText() : null;

                if (!name.isEmpty()) {
                    countries.add(new Country(name, code, flag));
                }
            }
        }

        return countries;
    }

    /**
     * Load leagues from /leagues endpoint.
     */
    private List<League> loadLeagues(ApiSportsHttpClient client, ObjectMapper mapper, ExecutionContext exec) throws Exception {
        List<League> leagues = new ArrayList<>();
        Map<String, String> params = new HashMap<>();

        waitForRateLimit(exec, "Loading leagues");
        String response = client.get("/leagues", params);
        JsonNode root = mapper.readTree(response);
        JsonNode responseArray = root.get("response");

        if (responseArray != null && responseArray.isArray()) {
            for (JsonNode item : responseArray) {
                JsonNode leagueNode = item.get("league");
                JsonNode countryNode = item.get("country");

                if (leagueNode != null) {
                    int id = leagueNode.has("id") ? leagueNode.get("id").asInt() : 0;
                    String name = leagueNode.has("name") ? leagueNode.get("name").asText() : "";
                    String type = leagueNode.has("type") ? leagueNode.get("type").asText() : "";
                    String logo = leagueNode.has("logo") ? leagueNode.get("logo").asText() : null;

                    String countryName = "";
                    if (countryNode != null && countryNode.has("name")) {
                        countryName = countryNode.get("name").asText();
                    }

                    if (id > 0 && !name.isEmpty()) {
                        leagues.add(new League(id, name, type, countryName, logo));
                    }
                }
            }
        }

        return leagues;
    }

    /**
     * Load leagues from /leagues endpoint filtered by countries.
     * Makes separate API calls for each country to reduce data transfer.
     */
    private List<League> loadLeaguesFiltered(ApiSportsHttpClient client, ObjectMapper mapper,
                                             Set<String> countries, ExecutionContext exec) throws Exception {
        List<League> leagues = new ArrayList<>();
        int countryCount = 0;

        for (String country : countries) {
            exec.checkCanceled();
            countryCount++;

            Map<String, String> params = new HashMap<>();
            params.put("country", country);

            try {
                waitForRateLimit(exec, "Loading leagues for " + country);
                String response = client.get("/leagues", params);
                JsonNode root = mapper.readTree(response);
                JsonNode responseArray = root.get("response");

                if (responseArray != null && responseArray.isArray()) {
                    for (JsonNode item : responseArray) {
                        JsonNode leagueNode = item.get("league");
                        JsonNode countryNode = item.get("country");

                        if (leagueNode != null) {
                            int id = leagueNode.has("id") ? leagueNode.get("id").asInt() : 0;
                            String name = leagueNode.has("name") ? leagueNode.get("name").asText() : "";
                            String type = leagueNode.has("type") ? leagueNode.get("type").asText() : "";
                            String logo = leagueNode.has("logo") ? leagueNode.get("logo").asText() : null;

                            String countryName = "";
                            if (countryNode != null && countryNode.has("name")) {
                                countryName = countryNode.get("name").asText();
                            }

                            if (id > 0 && !name.isEmpty()) {
                                leagues.add(new League(id, name, type, countryName, logo));
                            }
                        }
                    }
                }

            } catch (Exception e) {
                getLogger().warn("Failed to load leagues for country " + country + ": " + e.getMessage());
            }

            // Update progress
            if (countryCount % 2 == 0) {
                double progress = 0.2 + (0.05 * ((double) countryCount / countries.size()));
                exec.setProgress(progress, "Loading leagues for " + country + "... (" +
                                countryCount + "/" + countries.size() + " countries)");
            }
        }

        return leagues;
    }

    /**
     * Load seasons from /leagues endpoint for each league.
     * Seasons are embedded in the league response.
     */
    private List<Season> loadSeasons(ApiSportsHttpClient client, ObjectMapper mapper,
                                      List<League> leagues, ExecutionContext exec) throws Exception {
        List<Season> allSeasons = new ArrayList<>();

        for (int i = 0; i < leagues.size(); i++) {
            exec.checkCanceled();
            League league = leagues.get(i);

            Map<String, String> params = new HashMap<>();
            params.put("id", String.valueOf(league.getId()));

            try {
                waitForRateLimit(exec, "Loading seasons for " + league.getName());
                String response = client.get("/leagues", params);
                JsonNode root = mapper.readTree(response);
                JsonNode responseArray = root.get("response");

                if (responseArray != null && responseArray.isArray() && responseArray.size() > 0) {
                    JsonNode leagueData = responseArray.get(0);
                    JsonNode seasonsArray = leagueData.get("seasons");

                    if (seasonsArray != null && seasonsArray.isArray()) {
                        for (JsonNode seasonNode : seasonsArray) {
                            int year = seasonNode.has("year") ? seasonNode.get("year").asInt() : 0;
                            String start = seasonNode.has("start") ? seasonNode.get("start").asText() : null;
                            String end = seasonNode.has("end") ? seasonNode.get("end").asText() : null;
                            boolean current = seasonNode.has("current") && seasonNode.get("current").asBoolean();

                            if (year >= 2008) { // Filter seasons from 2008 onwards
                                allSeasons.add(new Season(league.getId(), year, start, end, current));
                            }
                        }
                    }
                }

            } catch (Exception e) {
                getLogger().warn("Failed to load seasons for league " + league.getName() + ": " + e.getMessage());
            }

            // Update progress
            if (i % 10 == 0) {
                double progress = 0.4 + (0.2 * ((double) i / leagues.size()));
                exec.setProgress(progress, "Loading seasons... (" + (i + 1) + "/" + leagues.size() + ")");
            }
        }

        return allSeasons;
    }

    /**
     * Load teams from /teams endpoint for each league.
     * Loads teams for all filtered leagues using the most recent season.
     */
    private List<Team> loadTeams(ApiSportsHttpClient client, ObjectMapper mapper,
                                  List<League> leagues, ExecutionContext exec) throws Exception {
        List<Team> teams = new ArrayList<>();
        Map<Integer, Team> teamMap = new HashMap<>(); // Deduplicate teams by ID

        // Determine which season to use for team queries
        // Use the most recent year from filtered seasons, or current year
        int seasonToUse = java.time.Year.now().getValue();
        String[] selectedSeasons = m_selectedSeasons.getStringArrayValue();
        if (selectedSeasons.length > 0) {
            // Use the first selected season
            try {
                seasonToUse = Integer.parseInt(selectedSeasons[0]);
            } catch (NumberFormatException e) {
                // Use current year
            }
        }

        getLogger().info("Loading teams for " + leagues.size() + " leagues using season " + seasonToUse);

        // Load teams for ALL filtered leagues
        for (int i = 0; i < leagues.size(); i++) {
            exec.checkCanceled();
            League league = leagues.get(i);

            Map<String, String> params = new HashMap<>();
            params.put("league", String.valueOf(league.getId()));
            params.put("season", String.valueOf(seasonToUse));

            try {
                waitForRateLimit(exec, "Loading teams for " + league.getName());
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

            // Update progress
            if (i % 5 == 0) {
                double progress = 0.6 + (0.2 * ((double) i / leagues.size()));
                exec.setProgress(progress, "Loading teams... (" + (i + 1) + "/" + leagues.size() + " leagues)");
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
}
