package com.apisports.knime.football.nodes.universal;

import com.apisports.knime.core.descriptor.DescriptorRegistry;
import com.apisports.knime.core.descriptor.EndpointDescriptor;
import com.apisports.knime.port.ReferenceDAO;
import com.apisports.knime.port.ReferenceData;
import com.apisports.knime.port.ReferenceDataPortObjectSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom dialog for Universal Node with cascading dropdowns.
 * Populates League, Season, and Team dropdowns from Reference Data input.
 */
public class UniversalNodeDialog extends NodeDialogPane {

    private JComboBox<String> endpointCombo;
    private JComboBox<LeagueItem> leagueCombo;
    private JComboBox<Integer> seasonCombo;
    private JComboBox<TeamItem> teamCombo;
    private JTextArea parametersArea;

    private List<ReferenceData.League> allLeagues = new ArrayList<>();
    private List<ReferenceData.Season> allSeasons = new ArrayList<>();
    private List<ReferenceData.Team> allTeams = new ArrayList<>();

    public UniversalNodeDialog() {
        super();
        addTab("Query Configuration", createMainPanel());
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Endpoint selection
        panel.add(createEndpointPanel());
        panel.add(Box.createVerticalStrut(10));

        // Reference data selections
        panel.add(createReferenceDataPanel());
        panel.add(Box.createVerticalStrut(10));

        // Additional parameters
        panel.add(createParametersPanel());

        return panel;
    }

    private JPanel createEndpointPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Endpoint Selection"));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));

        panel.add(new JLabel("Endpoint:"));
        endpointCombo = new JComboBox<>();
        endpointCombo.setPreferredSize(new Dimension(300, 25));
        loadEndpoints();
        panel.add(endpointCombo);

        return panel;
    }

    private JPanel createReferenceDataPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Reference Data Selections"));
        panel.setLayout(new GridLayout(3, 2, 10, 10));

        // League selection
        panel.add(new JLabel("League:"));
        leagueCombo = new JComboBox<>();
        leagueCombo.setPreferredSize(new Dimension(300, 25));
        leagueCombo.addActionListener(e -> onLeagueChanged());
        panel.add(leagueCombo);

        // Season selection
        panel.add(new JLabel("Season:"));
        seasonCombo = new JComboBox<>();
        seasonCombo.setPreferredSize(new Dimension(300, 25));
        panel.add(seasonCombo);

        // Team selection
        panel.add(new JLabel("Team (optional):"));
        teamCombo = new JComboBox<>();
        teamCombo.setPreferredSize(new Dimension(300, 25));
        panel.add(teamCombo);

        return panel;
    }

    private JPanel createParametersPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Additional Parameters (JSON)"));
        panel.setLayout(new BorderLayout());

        parametersArea = new JTextArea(5, 40);
        parametersArea.setText("{}");
        JScrollPane scrollPane = new JScrollPane(parametersArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JLabel helpLabel = new JLabel("<html>Enter additional parameters as JSON. Examples:<br>" +
            "{\"date\":\"2024-12-20\", \"status\":\"FT\"}<br>" +
            "{\"live\":\"all\"}</html>");
        panel.add(helpLabel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadEndpoints() {
        try {
            DescriptorRegistry registry = DescriptorRegistry.getInstance();
            if (registry.getAllDescriptors().isEmpty()) {
                java.io.InputStream stream = getClass().getResourceAsStream("/descriptors/football-endpoints.yaml");
                if (stream != null) {
                    registry.loadFromStream(stream);
                }
            }

            List<String> endpoints = registry.getAllDescriptors()
                .stream()
                .map(EndpointDescriptor::getId)
                .sorted()
                .collect(Collectors.toList());

            endpointCombo.removeAllItems();
            for (String endpoint : endpoints) {
                endpointCombo.addItem(endpoint);
            }

            if (endpointCombo.getItemCount() == 0) {
                endpointCombo.addItem("<no endpoints loaded>");
            }
        } catch (Exception e) {
            e.printStackTrace();
            endpointCombo.addItem("<error loading endpoints>");
        }
    }

    private void onLeagueChanged() {
        LeagueItem selectedLeague = (LeagueItem) leagueCombo.getSelectedItem();
        if (selectedLeague == null) {
            return;
        }

        // Update seasons for selected league
        updateSeasonCombo(selectedLeague.id);

        // Update teams for selected league
        updateTeamCombo(selectedLeague.id);
    }

    private void updateSeasonCombo(int leagueId) {
        seasonCombo.removeAllItems();
        seasonCombo.addItem(-1); // "All seasons" option

        for (ReferenceData.Season season : allSeasons) {
            if (season.getLeagueId() == leagueId) {
                seasonCombo.addItem(season.getYear());
            }
        }
    }

    private void updateTeamCombo(int leagueId) {
        teamCombo.removeAllItems();
        teamCombo.addItem(new TeamItem(-1, "<None>")); // Optional team

        for (ReferenceData.Team team : allTeams) {
            if (team.getLeagueIds().contains(leagueId)) {
                teamCombo.addItem(new TeamItem(team.getId(), team.getName()));
            }
        }
    }

    @Override
    protected void loadSettingsFrom(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {

        // Load reference data from input port
        if (specs.length > 1 && specs[1] instanceof ReferenceDataPortObjectSpec) {
            ReferenceDataPortObjectSpec refSpec = (ReferenceDataPortObjectSpec) specs[1];
            String dbPath = refSpec.getDbPath();

            if (dbPath != null && new File(dbPath).exists()) {
                loadReferenceData(dbPath);
            } else {
                throw new NotConfigurableException(
                    "Reference database not found at: " + dbPath + ". " +
                    "Please execute the Reference Data Loader node first.");
            }
        } else {
            throw new NotConfigurableException(
                "Reference Data port not connected. Please connect the Reference Data Loader node.");
        }

        // Load saved settings
        try {
            String endpoint = settings.getString(UniversalNodeModel.CFGKEY_ENDPOINT_ID, "");
            if (!endpoint.isEmpty()) {
                endpointCombo.setSelectedItem(endpoint);
            }

            int leagueId = settings.getInt(UniversalNodeModel.CFGKEY_SELECTED_LEAGUE, -1);
            if (leagueId > 0) {
                for (int i = 0; i < leagueCombo.getItemCount(); i++) {
                    LeagueItem item = leagueCombo.getItemAt(i);
                    if (item != null && item.id == leagueId) {
                        leagueCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }

            int season = settings.getInt(UniversalNodeModel.CFGKEY_SELECTED_SEASON, -1);
            if (season > 0) {
                seasonCombo.setSelectedItem(season);
            }

            int teamId = settings.getInt(UniversalNodeModel.CFGKEY_SELECTED_TEAM, -1);
            if (teamId > 0) {
                for (int i = 0; i < teamCombo.getItemCount(); i++) {
                    TeamItem item = teamCombo.getItemAt(i);
                    if (item != null && item.id == teamId) {
                        teamCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }

            String params = settings.getString(UniversalNodeModel.CFGKEY_PARAMETERS, "{}");
            parametersArea.setText(params);

        } catch (InvalidSettingsException e) {
            // Use defaults
        }
    }

    private void loadReferenceData(String dbPath) throws NotConfigurableException {
        try (ReferenceDAO dao = new ReferenceDAO(dbPath)) {
            // Load all leagues
            allLeagues = dao.getAllLeagues();
            leagueCombo.removeAllItems();
            for (ReferenceData.League league : allLeagues) {
                leagueCombo.addItem(new LeagueItem(league.getId(), league.getName(), league.getCountryName()));
            }

            // Load all seasons
            allSeasons = dao.getAllSeasons();

            // Load all teams
            allTeams = dao.getAllTeams();

            // Trigger initial cascade if a league is selected
            if (leagueCombo.getItemCount() > 0) {
                leagueCombo.setSelectedIndex(0);
            }

        } catch (Exception e) {
            throw new NotConfigurableException("Failed to load reference data: " + e.getMessage(), e);
        }
    }

    @Override
    protected void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
        // Save endpoint
        String endpoint = (String) endpointCombo.getSelectedItem();
        settings.addString(UniversalNodeModel.CFGKEY_ENDPOINT_ID, endpoint != null ? endpoint : "");

        // Save league
        LeagueItem league = (LeagueItem) leagueCombo.getSelectedItem();
        settings.addInt(UniversalNodeModel.CFGKEY_SELECTED_LEAGUE, league != null ? league.id : -1);

        // Save season
        Integer season = (Integer) seasonCombo.getSelectedItem();
        settings.addInt(UniversalNodeModel.CFGKEY_SELECTED_SEASON, season != null ? season : -1);

        // Save team
        TeamItem team = (TeamItem) teamCombo.getSelectedItem();
        settings.addInt(UniversalNodeModel.CFGKEY_SELECTED_TEAM, team != null ? team.id : -1);

        // Save parameters
        settings.addString(UniversalNodeModel.CFGKEY_PARAMETERS, parametersArea.getText());

        // Save country (not used in this dialog, but needed for backwards compatibility)
        settings.addString(UniversalNodeModel.CFGKEY_SELECTED_COUNTRY, "");
    }

    // Helper classes for combo box items
    private static class LeagueItem {
        final int id;
        final String name;
        final String country;

        LeagueItem(int id, String name, String country) {
            this.id = id;
            this.name = name;
            this.country = country;
        }

        @Override
        public String toString() {
            return name + " (" + country + ")";
        }
    }

    private static class TeamItem {
        final int id;
        final String name;

        TeamItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
