package com.apisports.knime.football.nodes.query.odds;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.util.HashMap;
import java.util.Map;

/**
 * NodeModel for Odds query node.
 * Queries betting odds from the Football API.
 */
public class OddsNodeModel extends AbstractFootballQueryNodeModel {

    static final String CFGKEY_QUERY_TYPE = "queryType";
    static final String CFGKEY_FIXTURE_ID = "fixtureId";
    static final String CFGKEY_BOOKMAKER = "bookmaker";
    static final String CFGKEY_BET_TYPE = "betType";

    // Query types
    static final String QUERY_BY_FIXTURE = "By Fixture ID";
    static final String QUERY_BY_LEAGUE = "By League/Season";
    static final String QUERY_LIVE = "Live Odds";

    protected final SettingsModelString m_queryType =
        new SettingsModelString(CFGKEY_QUERY_TYPE, QUERY_BY_FIXTURE);
    protected final SettingsModelString m_fixtureId =
        new SettingsModelString(CFGKEY_FIXTURE_ID, "");
    protected final SettingsModelString m_bookmaker =
        new SettingsModelString(CFGKEY_BOOKMAKER, "");
    protected final SettingsModelString m_betType =
        new SettingsModelString(CFGKEY_BET_TYPE, "");

    @Override
    protected void validateExecutionSettings() throws InvalidSettingsException {
        String queryType = m_queryType.getStringValue();

        if (QUERY_BY_FIXTURE.equals(queryType)) {
            if (m_fixtureId.getStringValue().isEmpty()) {
                throw new InvalidSettingsException("Please specify a fixture ID");
            }
        } else if (QUERY_BY_LEAGUE.equals(queryType)) {
            super.validateExecutionSettings(); // Validates league and season
        }
        // QUERY_LIVE has no special validation
    }

    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        exec.setMessage("Building query parameters...");

        // Build query parameters
        Map<String, String> params = buildQueryParams();

        // Make API call
        String endpoint = "/odds";
        if (QUERY_LIVE.equals(m_queryType.getStringValue())) {
            endpoint = "/odds/live";
        }

        exec.setMessage("Querying odds from API...");
        JsonNode response = callApi(client, endpoint, params, mapper);

        // Parse response and create output table
        exec.setMessage("Parsing results...");
        BufferedDataTable result = parseOddsResponse(response, exec);

        getLogger().info("Retrieved " + result.size() + " odds records");
        return result;
    }

    /**
     * Build query parameters based on query type.
     */
    private Map<String, String> buildQueryParams() {
        Map<String, String> params = new HashMap<>();
        String queryType = m_queryType.getStringValue();

        if (QUERY_BY_FIXTURE.equals(queryType)) {
            params.put("fixture", m_fixtureId.getStringValue());
        } else if (QUERY_BY_LEAGUE.equals(queryType)) {
            params.put("league", String.valueOf(m_leagueId.getIntValue()));
            params.put("season", String.valueOf(m_season.getIntValue()));
        }
        // QUERY_LIVE doesn't need parameters

        // Optional filters
        if (!m_bookmaker.getStringValue().isEmpty()) {
            params.put("bookmaker", m_bookmaker.getStringValue());
        }
        if (!m_betType.getStringValue().isEmpty()) {
            params.put("bet", m_betType.getStringValue());
        }

        return params;
    }

    /**
     * Parse odds API response and create output table.
     */
    private BufferedDataTable parseOddsResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);

        if (response != null && response.isArray()) {
            int rowNum = 0;

            for (JsonNode oddsItem : response) {
                try {
                    // Each odds item may have multiple bookmakers and bets
                    JsonNode fixture = oddsItem.get("fixture");
                    JsonNode league = oddsItem.get("league");
                    JsonNode bookmakers = oddsItem.get("bookmakers");

                    int fixtureId = fixture != null && fixture.has("id") ? fixture.get("id").asInt() : 0;
                    String leagueName = league != null && league.has("name") ? league.get("name").asText() : "";

                    if (bookmakers != null && bookmakers.isArray()) {
                        for (JsonNode bookmaker : bookmakers) {
                            String bookmakerId = bookmaker.has("id") ? String.valueOf(bookmaker.get("id").asInt()) : "";
                            String bookmakerName = bookmaker.has("name") ? bookmaker.get("name").asText() : "";

                            JsonNode bets = bookmaker.get("bets");
                            if (bets != null && bets.isArray()) {
                                for (JsonNode bet : bets) {
                                    String betName = bet.has("name") ? bet.get("name").asText() : "";

                                    JsonNode values = bet.get("values");
                                    if (values != null && values.isArray()) {
                                        for (JsonNode value : values) {
                                            DataRow row = parseOddsRow(fixtureId, leagueName, bookmakerId,
                                                                       bookmakerName, betName, value, rowNum);
                                            container.addRowToTable(row);
                                            rowNum++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    getLogger().warn("Failed to parse odds row: " + e.getMessage());
                }
            }
        }

        container.close();
        return container.getTable();
    }

    /**
     * Parse a single odds value into a DataRow.
     */
    private DataRow parseOddsRow(int fixtureId, String leagueName, String bookmakerId,
                                  String bookmakerName, String betName, JsonNode value, int rowNum) {
        String valueName = value.has("value") ? value.get("value").asText() : "";
        String odd = value.has("odd") ? value.get("odd").asText() : "";

        DataCell[] cells = new DataCell[]{
            new IntCell(fixtureId),
            new StringCell(leagueName),
            new StringCell(bookmakerId),
            new StringCell(bookmakerName),
            new StringCell(betName),
            new StringCell(valueName),
            new StringCell(odd)
        };

        return new DefaultRow(new RowKey("Row" + rowNum), cells);
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Fixture_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("League", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Bookmaker_ID", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Bookmaker_Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Bet_Type", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Bet_Value", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Odd", StringCell.TYPE).createSpec()
        );
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_queryType.saveSettingsTo(settings);
        m_fixtureId.saveSettingsTo(settings);
        m_bookmaker.saveSettingsTo(settings);
        m_betType.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_queryType.validateSettings(settings);
        m_fixtureId.validateSettings(settings);
        m_bookmaker.validateSettings(settings);
        m_betType.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_queryType.loadSettingsFrom(settings);
        m_fixtureId.loadSettingsFrom(settings);
        m_bookmaker.loadSettingsFrom(settings);
        m_betType.loadSettingsFrom(settings);
    }
}
