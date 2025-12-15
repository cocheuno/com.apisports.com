package com.apisports.knime.football.nodes.fixtures;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.port.ApiSportsConnectionPortObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FootballFixturesNodeModel extends NodeModel {
    static final String CFGKEY_LEAGUE_ID = "leagueId";
    static final String CFGKEY_SEASON = "season";
    
    private final SettingsModelInteger m_leagueId = new SettingsModelInteger(CFGKEY_LEAGUE_ID, 1);
    private final SettingsModelInteger m_season = new SettingsModelInteger(CFGKEY_SEASON, 2024);

    protected FootballFixturesNodeModel() {
        super(new PortType[]{ApiSportsConnectionPortObject.TYPE}, 
              new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        ApiSportsConnectionPortObject connection = (ApiSportsConnectionPortObject) inObjects[0];
        ApiSportsHttpClient client = connection.getClient();
        int leagueId = m_leagueId.getIntValue();
        int season = m_season.getIntValue();

        // Validate parameters
        if (leagueId <= 0) {
            throw new InvalidSettingsException("League ID must be greater than 0");
        }
        if (season < 2000 || season > 2100) {
            throw new InvalidSettingsException("Season must be a valid year (2000-2100)");
        }

        // Call API to get fixtures
        exec.setMessage("Fetching fixtures for league " + leagueId + " season " + season + "...");
        Map<String, String> params = new HashMap<>();
        params.put("league", String.valueOf(leagueId));
        params.put("season", String.valueOf(season));

        String responseBody;
        try {
            responseBody = client.get("/fixtures", params);
        } catch (Exception e) {
            throw new Exception("Failed to fetch fixtures from API: " + e.getMessage(), e);
        }

        // Parse JSON response using Jackson
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonResponse;
        JsonNode fixturesArray;
        try {
            jsonResponse = mapper.readTree(responseBody);

            // Check for API errors
            if (jsonResponse.has("errors") && jsonResponse.get("errors").size() > 0) {
                JsonNode errors = jsonResponse.get("errors");
                StringBuilder errorMsg = new StringBuilder("API returned errors: ");
                for (JsonNode error : errors) {
                    errorMsg.append(error.asText()).append("; ");
                }
                throw new Exception(errorMsg.toString());
            }

            fixturesArray = jsonResponse.get("response");
        } catch (Exception e) {
            throw new Exception("Failed to parse API response: " + e.getMessage() + ". Response: " +
                              (responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody), e);
        }

        // Create output table spec
        DataTableSpec outputSpec = createOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(outputSpec);

        // Process each fixture
        int fixtureCount = fixturesArray.size();
        exec.setMessage("Processing " + fixtureCount + " fixtures...");
        for (int i = 0; i < fixtureCount; i++) {
            exec.checkCanceled();
            exec.setProgress((double) i / fixtureCount, "Processing fixture " + (i + 1) + " of " + fixtureCount);

            try {
                JsonNode fixtureData = fixturesArray.get(i);
                JsonNode fixture = fixtureData.get("fixture");
                JsonNode teams = fixtureData.get("teams");

                int fixtureId = fixture.get("id").asInt();
                String date = fixture.get("date").asText();
                String status = fixture.get("status").get("long").asText();
                String homeTeam = teams.get("home").get("name").asText();
                String awayTeam = teams.get("away").get("name").asText();

                // Create row
                container.addRowToTable(new DefaultRow("Row" + i,
                    new IntCell(fixtureId),
                    new StringCell(homeTeam),
                    new StringCell(awayTeam),
                    new StringCell(date),
                    new StringCell(status)));
            } catch (Exception e) {
                getLogger().warn("Skipping fixture at index " + i + " due to error: " + e.getMessage());
            }
        }

        container.close();
        exec.setMessage("Complete - processed " + container.getTable().size() + " fixtures");
        return new PortObject[]{container.getTable()};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{createOutputSpec()};
    }

    private DataTableSpec createOutputSpec() {
        return new DataTableSpec(
            new String[]{"Fixture ID", "Home Team", "Away Team", "Date", "Status"},
            new org.knime.core.data.DataType[]{
                IntCell.TYPE, StringCell.TYPE, StringCell.TYPE, StringCell.TYPE, StringCell.TYPE
            });
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_leagueId.saveSettingsTo(settings);
        m_season.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_leagueId.loadSettingsFrom(settings);
        m_season.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_leagueId.validateSettings(settings);
        m_season.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException {
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException {
    }

    @Override
    protected void reset() {
    }
}
