package com.apisports.knime.football.nodes.referencedata;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.port.ApiSportsConnectionPortObject;
import com.apisports.knime.port.ReferenceData;
import com.apisports.knime.port.ReferenceData.Country;
import com.apisports.knime.port.ReferenceData.League;
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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NodeModel for Reference Data Loader.
 * Loads countries, leagues, teams, and venues from the API.
 */
public class ReferenceDataLoaderNodeModel extends NodeModel {

    static final String CFGKEY_LOAD_TEAMS = "loadTeams";
    static final String CFGKEY_LOAD_VENUES = "loadVenues";
    static final String CFGKEY_CACHE_TTL = "cacheTtl";

    private final SettingsModelBoolean m_loadTeams =
        new SettingsModelBoolean(CFGKEY_LOAD_TEAMS, true);
    private final SettingsModelBoolean m_loadVenues =
        new SettingsModelBoolean(CFGKEY_LOAD_VENUES, false);
    private final SettingsModelInteger m_cacheTtl =
        new SettingsModelInteger(CFGKEY_CACHE_TTL, 86400); // 24 hours default

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

        // Load countries
        exec.setMessage("Loading countries...");
        exec.setProgress(0.1);
        List<Country> countries = loadCountries(client, mapper);
        getLogger().info("Loaded " + countries.size() + " countries");

        // Load leagues
        exec.setMessage("Loading leagues...");
        exec.setProgress(0.3);
        List<League> leagues = loadLeagues(client, mapper);
        getLogger().info("Loaded " + leagues.size() + " leagues");

        // Load teams if enabled
        List<Team> teams = new ArrayList<>();
        if (m_loadTeams.getBooleanValue()) {
            exec.setMessage("Loading teams...");
            exec.setProgress(0.5);
            teams = loadTeams(client, mapper, leagues, exec);
            getLogger().info("Loaded " + teams.size() + " teams");
        }

        // Load venues if enabled
        List<Venue> venues = new ArrayList<>();
        if (m_loadVenues.getBooleanValue()) {
            exec.setMessage("Loading venues...");
            exec.setProgress(0.8);
            venues = loadVenues(client, mapper);
            getLogger().info("Loaded " + venues.size() + " venues");
        }

        // Create reference data
        exec.setProgress(1.0);
        ReferenceData refData = new ReferenceData(countries, leagues, teams, venues);
        ReferenceDataPortObject output = new ReferenceDataPortObject(refData);

        return new PortObject[]{output};
    }

    /**
     * Load countries from /countries endpoint.
     */
    private List<Country> loadCountries(ApiSportsHttpClient client, ObjectMapper mapper) throws Exception {
        List<Country> countries = new ArrayList<>();
        Map<String, String> params = new HashMap<>();

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
    private List<League> loadLeagues(ApiSportsHttpClient client, ObjectMapper mapper) throws Exception {
        List<League> leagues = new ArrayList<>();
        Map<String, String> params = new HashMap<>();

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
     * Load teams from /teams endpoint for each league.
     * This can be expensive, so we limit to major leagues or use a sample.
     */
    private List<Team> loadTeams(ApiSportsHttpClient client, ObjectMapper mapper,
                                  List<League> leagues, ExecutionContext exec) throws Exception {
        List<Team> teams = new ArrayList<>();
        Map<Integer, Team> teamMap = new HashMap<>(); // Deduplicate teams by ID

        // For prototype, only load teams from first 10 leagues to avoid excessive API calls
        // TODO: Add configuration for which leagues to load
        int leagueLimit = Math.min(10, leagues.size());

        for (int i = 0; i < leagueLimit; i++) {
            exec.checkCanceled();
            League league = leagues.get(i);

            Map<String, String> params = new HashMap<>();
            params.put("league", String.valueOf(league.getId()));
            params.put("season", "2024"); // TODO: Make season configurable

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

                // Small delay to avoid rate limiting
                Thread.sleep(100);

            } catch (Exception e) {
                getLogger().warn("Failed to load teams for league " + league.getName() + ": " + e.getMessage());
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
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_loadTeams.validateSettings(settings);
        m_loadVenues.validateSettings(settings);
        m_cacheTtl.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_loadTeams.loadSettingsFrom(settings);
        m_loadVenues.loadSettingsFrom(settings);
        m_cacheTtl.loadSettingsFrom(settings);
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
