package com.apisports.knime.football.nodes.leagues;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.port.ApiSportsConnectionPortObject;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * NodeModel for the Football Leagues node.
 */
public class FootballLeaguesNodeModel extends NodeModel {

    static final String CFGKEY_COUNTRY = "country";
    
    private final SettingsModelString m_country = new SettingsModelString(CFGKEY_COUNTRY, "");

    protected FootballLeaguesNodeModel() {
        super(new PortType[]{ApiSportsConnectionPortObject.TYPE}, 
              new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        ApiSportsConnectionPortObject connection = (ApiSportsConnectionPortObject) inObjects[0];
        ApiSportsHttpClient client = connection.getClient();
        String country = m_country.getStringValue();

        // Validate country parameter
        if (country == null || country.trim().isEmpty()) {
            throw new InvalidSettingsException("Country parameter must not be empty");
        }

        // Call API to get leagues
        Map<String, String> params = new HashMap<>();
        params.put("country", country);
        String responseBody = client.get("/leagues", params);

        // Parse JSON response
        JSONObject jsonResponse = new JSONObject(responseBody);
        JSONArray leagues = jsonResponse.getJSONArray("response");

        // Create output table spec
        DataTableSpec outputSpec = createOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(outputSpec);

        // Process each league
        for (int i = 0; i < leagues.length(); i++) {
            JSONObject leagueData = leagues.getJSONObject(i);
            JSONObject league = leagueData.getJSONObject("league");
            JSONObject countryObj = leagueData.getJSONObject("country");

            int leagueId = league.getInt("id");
            String leagueName = league.getString("name");
            String leagueType = league.getString("type");
            String countryName = countryObj.getString("name");

            // Find current season
            int currentYear = 0;
            if (leagueData.has("seasons")) {
                JSONArray seasons = leagueData.getJSONArray("seasons");
                for (int j = 0; j < seasons.length(); j++) {
                    JSONObject season = seasons.getJSONObject(j);
                    if (season.has("current") && season.getBoolean("current")) {
                        currentYear = season.getInt("year");
                        break;
                    }
                }
                // If no current season found, use the last season
                if (currentYear == 0 && seasons.length() > 0) {
                    JSONObject lastSeason = seasons.getJSONObject(seasons.length() - 1);
                    currentYear = lastSeason.getInt("year");
                }
            }

            // Create row
            container.addRowToTable(new DefaultRow("Row" + i,
                new IntCell(leagueId),
                new StringCell(leagueName),
                new StringCell(countryName),
                new StringCell(leagueType),
                new IntCell(currentYear)));
        }

        container.close();
        return new PortObject[]{container.getTable()};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{createOutputSpec()};
    }

    private DataTableSpec createOutputSpec() {
        return new DataTableSpec(
            new String[]{"League ID", "Name", "Country", "Type", "Season"},
            new org.knime.core.data.DataType[]{
                IntCell.TYPE, StringCell.TYPE, StringCell.TYPE, StringCell.TYPE, IntCell.TYPE
            });
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_country.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_country.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_country.validateSettings(settings);
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
