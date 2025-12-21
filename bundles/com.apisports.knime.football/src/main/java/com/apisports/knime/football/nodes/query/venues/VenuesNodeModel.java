package com.apisports.knime.football.nodes.query.venues;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import java.util.*;

public class VenuesNodeModel extends AbstractFootballQueryNodeModel {

    static final String CFGKEY_VENUE_NAME = "venueName";
    static final String CFGKEY_CITY = "city";

    protected final SettingsModelString m_venueName =
        new SettingsModelString(CFGKEY_VENUE_NAME, "");
    protected final SettingsModelString m_city =
        new SettingsModelString(CFGKEY_CITY, "");

    @Override
    protected void validateExecutionSettings() throws InvalidSettingsException {
        // At least one search criteria needed
        if (m_venueName.getStringValue().isEmpty() && 
            m_city.getStringValue().isEmpty() &&
            m_leagueId.getIntValue() <= 0) {
            throw new InvalidSettingsException("Please specify venue name, city, or country");
        }
    }

    @Override
    protected BufferedDataTable executeQuery(ApiSportsHttpClient client, ObjectMapper mapper,
                                              ExecutionContext exec) throws Exception {
        Map<String, String> params = new HashMap<>();
        
        if (!m_venueName.getStringValue().isEmpty()) {
            params.put("name", m_venueName.getStringValue());
        }
        if (!m_city.getStringValue().isEmpty()) {
            params.put("city", m_city.getStringValue());
        }
        if (m_leagueId.getIntValue() > 0) {
            String countryName = getLeagueName(m_leagueId.getIntValue());
            if (countryName.contains("(") && countryName.contains(")")) {
                String country = countryName.substring(countryName.lastIndexOf("(") + 1, countryName.lastIndexOf(")"));
                params.put("country", country);
            }
        }

        exec.setMessage("Querying venues from API...");
        JsonNode response = callApi(client, "/venues", params, mapper);

        return parseVenuesResponse(response, exec);
    }

    private BufferedDataTable parseVenuesResponse(JsonNode response, ExecutionContext exec) {
        DataTableSpec spec = getOutputSpec();
        BufferedDataContainer container = exec.createDataContainer(spec);
        int rowNum = 0;

        if (response != null && response.isArray()) {
            for (JsonNode item : response) {
                try {
                    int id = item.has("id") ? item.get("id").asInt() : 0;
                    String name = item.has("name") ? item.get("name").asText() : "";
                    String address = item.has("address") ? item.get("address").asText() : "";
                    String city = item.has("city") ? item.get("city").asText() : "";
                    String country = item.has("country") ? item.get("country").asText() : "";
                    String capacity = item.has("capacity") && !item.get("capacity").isNull()
                        ? String.valueOf(item.get("capacity").asInt()) : "";
                    String surface = item.has("surface") ? item.get("surface").asText() : "";
                    String image = item.has("image") ? item.get("image").asText() : "";

                    DataCell[] cells = new DataCell[]{
                        new IntCell(id),
                        new StringCell(name),
                        new StringCell(address),
                        new StringCell(city),
                        new StringCell(country),
                        new StringCell(capacity),
                        new StringCell(surface),
                        new StringCell(image)
                    };

                    container.addRowToTable(new DefaultRow(new RowKey("Row" + rowNum), cells));
                    rowNum++;
                } catch (Exception e) {
                    getLogger().warn("Failed to parse venue: " + e.getMessage());
                }
            }
        }

        container.close();
        return container.getTable();
    }

    @Override
    protected DataTableSpec getOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("Venue_ID", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Address", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("City", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Country", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Capacity", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Surface", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Image_URL", StringCell.TYPE).createSpec()
        );
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_venueName.saveSettingsTo(settings);
        m_city.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_venueName.validateSettings(settings);
        m_city.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_venueName.loadSettingsFrom(settings);
        m_city.loadSettingsFrom(settings);
    }
}
