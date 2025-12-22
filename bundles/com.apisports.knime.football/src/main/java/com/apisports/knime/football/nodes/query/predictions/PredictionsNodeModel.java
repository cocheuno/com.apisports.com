package com.apisports.knime.football.nodes.query.predictions;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeModel;
import com.apisports.knime.port.ApiSportsConnectionPortObject;
import com.apisports.knime.port.ReferenceDataPortObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import java.util.*;

public class PredictionsNodeModel extends AbstractFootballQueryNodeModel {

    static final String CFGKEY_FIXTURE_ID = "fixtureId";

    protected final SettingsModelString m_fixtureId =
        new SettingsModelString(CFGKEY_FIXTURE_ID, "");

    /**
     * Constructor that adds an optional third input port for fixtures data.
     */
    public PredictionsNodeModel() {
        super(
            new PortType[]{
                ApiSportsConnectionPortObject.TYPE,
                ReferenceDataPortObject.TYPE,
                new PortType(BufferedDataTable.class, true) // Optional fixtures input
            },
            new PortType[]{
                BufferedDataTable.TYPE
            }
        );
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        // Get API client from connection port
        ApiSportsConnectionPortObject connectionPort = (ApiSportsConnectionPortObject) inObjects[0];
        ApiSportsHttpClient client = connectionPort.getClient();

        // Get reference data from port
        ReferenceDataPortObject refDataPort = (ReferenceDataPortObject) inObjects[1];
        m_dbPath = refDataPort.getDbPath();

        // Load reference data from database
        loadReferenceData();

        // Note: Optional fixtures port (inObjects[2]) is only used by the dialog
        // for populating the fixture dropdown. Not needed during execution.

        // Validate settings
        validateExecutionSettings();

        // Execute endpoint-specific query
        BufferedDataTable result = executeQuery(client, new ObjectMapper(), exec);

        return new PortObject[]{result};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // Check first two required ports
        if (inSpecs[0] == null) {
            throw new InvalidSettingsException("API connection required");
        }
        if (inSpecs[1] == null) {
            throw new InvalidSettingsException("Reference data required");
        }

        // Third port (fixtures) is optional, no check needed

        return new PortObjectSpec[]{getOutputSpec()};
    }

    @Override
    protected void validateExecutionSettings() throws InvalidSettingsException {
        if (m_fixtureId.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("Please specify a fixture ID for prediction");
        }
    }

    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("fixture", m_fixtureId.getStringValue());

        exec.setMessage("Querying predictions from API...");
        JsonNode response = callApi(client, "/predictions", params, mapper);
        return parseResponse(response, exec);
    }

    private BufferedDataTable parseResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);
        int rowNum = 0;

        if (response != null && response.isArray()) {
            for (JsonNode item : response) {
                try {
                    JsonNode predictions = item.get("predictions");
                    JsonNode league = item.get("league");
                    JsonNode teams = item.get("teams");

                    int fixtureId = 0;
                    if (item.has("fixture") && item.get("fixture").has("id")) {
                        fixtureId = item.get("fixture").get("id").asInt();
                    }

                    String leagueName = league != null && league.has("name") ? league.get("name").asText() : "";
                    String winner = predictions != null && predictions.has("winner") && predictions.get("winner").has("name")
                        ? predictions.get("winner").get("name").asText() : "";
                    String winPercent = predictions != null && predictions.has("percent") && predictions.get("percent").has(winner.toLowerCase())
                        ? predictions.get("percent").get(winner.toLowerCase()).asText() : "";
                    String advice = predictions != null && predictions.has("advice") ? predictions.get("advice").asText() : "";

                    String homeTeam = teams != null && teams.has("home") && teams.get("home").has("name")
                        ? teams.get("home").get("name").asText() : "";
                    String awayTeam = teams != null && teams.has("away") && teams.get("away").has("name")
                        ? teams.get("away").get("away").get("name").asText() : "";

                    DataCell[] cells = new DataCell[]{
                        new IntCell(fixtureId),
                        new StringCell(leagueName),
                        new StringCell(homeTeam),
                        new StringCell(awayTeam),
                        new StringCell(winner),
                        new StringCell(winPercent),
                        new StringCell(advice)
                    };
                    container.addRowToTable(new DefaultRow(new RowKey("Row" + rowNum), cells));
                    rowNum++;
                } catch (Exception e) {
                    getLogger().warn("Failed to parse prediction: " + e.getMessage());
                }
            }
        }
        container.close();
        return container.getTable();
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Fixture_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("League", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Home_Team", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Away_Team", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Winner", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Win_Percent", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Advice", StringCell.TYPE).createSpec()
        );
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_fixtureId.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_fixtureId.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_fixtureId.loadSettingsFrom(settings);
    }
}
