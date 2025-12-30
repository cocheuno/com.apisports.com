package com.apisports.knime.football.nodes.query.fixtures;

import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeDialog;
import com.apisports.knime.football.ui.DateRangePanel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * Dialog for Fixtures query node.
 */
public class FixturesNodeDialog extends AbstractFootballQueryNodeDialog {

    private JComboBox<String> queryTypeCombo;
    private DateRangePanel dateRangePanel;  // NEW: Replaces simple text fields
    private JTextField fixtureIdField;
    private JComboBox<String> statusCombo;
    private JCheckBox includeEventsCheck;
    private JCheckBox includeLineupsCheck;
    private JCheckBox includeStatisticsCheck;
    private JCheckBox includePlayerStatsCheck;
    private JComboBox<TeamItem> team2Combo;

    // Panels that show/hide based on query type
    private JPanel fixtureIdPanel;
    private JPanel h2hPanel;

    public FixturesNodeDialog() {
        super();
        addFixturesSpecificComponents();
    }

    private void addFixturesSpecificComponents() {
        // Add separator
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        mainPanel.add(Box.createVerticalStrut(10));

        // Query type selection
        JPanel queryTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        queryTypePanel.add(new JLabel("Query Type:"));
        queryTypeCombo = new JComboBox<>(new String[]{
            FixturesNodeModel.QUERY_BY_LEAGUE,
            FixturesNodeModel.QUERY_BY_DATE,
            FixturesNodeModel.QUERY_BY_TEAM,
            FixturesNodeModel.QUERY_BY_ID,
            FixturesNodeModel.QUERY_LIVE,
            FixturesNodeModel.QUERY_H2H
        });
        queryTypeCombo.setPreferredSize(new Dimension(200, 25));
        queryTypeCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateVisibilityForQueryType();
            }
        });
        queryTypePanel.add(queryTypeCombo);
        mainPanel.add(queryTypePanel);

        // Date range panel - NEW: Use DateRangePanel with incremental mode enabled
        // Incremental mode enabled for Fixtures (details node) - enables daily/weekly refresh use cases
        dateRangePanel = new DateRangePanel("lastFixtureQuery", true);
        mainPanel.add(dateRangePanel);

        // Fixture ID panel (for ID queries)
        fixtureIdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fixtureIdPanel.add(new JLabel("Fixture ID:"));
        fixtureIdField = new JTextField(15);
        fixtureIdPanel.add(fixtureIdField);
        mainPanel.add(fixtureIdPanel);

        // H2H panel (for head-to-head queries - shows second team dropdown)
        h2hPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        h2hPanel.add(new JLabel("Team 2:"));
        team2Combo = new JComboBox<>();
        team2Combo.setPreferredSize(new Dimension(300, 25));
        h2hPanel.add(team2Combo);
        mainPanel.add(h2hPanel);

        // Status filter
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("Status Filter:"));
        statusCombo = new JComboBox<>(new String[]{
            "",
            "TBD",
            "NS",  // Not Started
            "1H",  // First Half
            "HT",  // Halftime
            "2H",  // Second Half
            "ET",  // Extra Time
            "P",   // Penalty
            "FT",  // Finished
            "AET", // After Extra Time
            "PEN", // Finished After Penalty
            "BT",  // Break Time
            "SUSP", // Suspended
            "INT",  // Interrupted
            "PST",  // Postponed
            "CANC", // Cancelled
            "ABD",  // Abandoned
            "AWD",  // Technical Loss
            "WO",   // WalkOver
            "LIVE"  // In Progress
        });
        statusCombo.setPreferredSize(new Dimension(150, 25));
        statusPanel.add(statusCombo);
        statusPanel.add(new JLabel("(Optional)"));
        mainPanel.add(statusPanel);

        // Include options
        JPanel includePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        includePanel.add(new JLabel("Include Additional Data:"));
        includeEventsCheck = new JCheckBox("Events");
        includeLineupsCheck = new JCheckBox("Lineups");
        includeStatisticsCheck = new JCheckBox("Statistics");
        includePlayerStatsCheck = new JCheckBox("Player Stats");
        includePanel.add(includeEventsCheck);
        includePanel.add(includeLineupsCheck);
        includePanel.add(includeStatisticsCheck);
        includePanel.add(includePlayerStatsCheck);
        mainPanel.add(includePanel);

        // Add help text
        JTextArea helpText = new JTextArea(
            "Query Types:\n" +
            "• By League/Season: Get all fixtures for a league and season\n" +
            "• By Date Range: Get fixtures within a date range (requires season)\n" +
            "• By Team: Get all fixtures for a specific team in a season\n" +
            "• By Fixture ID: Get a specific fixture by its ID\n" +
            "• Live Fixtures: Get all currently live fixtures\n" +
            "• Head to Head: Compare two teams in a specific season\n\n" +
            "Note: Dates accept YYYY-MM-DD or YYYY/MM/DD format"
        );
        helpText.setEditable(false);
        helpText.setWrapStyleWord(true);
        helpText.setLineWrap(true);
        helpText.setBackground(mainPanel.getBackground());
        helpText.setFont(new Font("SansSerif", Font.ITALIC, 10));
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(helpText);

        // Initial visibility
        updateVisibilityForQueryType();
    }

    @Override
    protected void onLeagueChanged() {
        super.onLeagueChanged();
        // Also populate team2Combo with the same teams for H2H queries
        populateTeam2Combo();
    }

    /**
     * Populate team2 combo with teams from selected league.
     */
    private void populateTeam2Combo() {
        LeagueItem selectedLeague = (LeagueItem) leagueCombo.getSelectedItem();
        if (selectedLeague == null) {
            return;
        }

        team2Combo.removeAllItems();
        team2Combo.addItem(new TeamItem(-1, "-- Select Team 2 --"));

        if (allTeams != null) {
            for (com.apisports.knime.port.ReferenceData.Team team : allTeams) {
                if (team.getLeagueIds().contains(selectedLeague.id)) {
                    team2Combo.addItem(new TeamItem(team.getId(), team.getName()));
                }
            }
        }
    }

    /**
     * Select team2 by ID in the combo box.
     */
    private void selectTeam2(int team2Id) {
        for (int i = 0; i < team2Combo.getItemCount(); i++) {
            TeamItem item = team2Combo.getItemAt(i);
            if (item.id == team2Id) {
                team2Combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Update panel visibility based on selected query type.
     */
    private void updateVisibilityForQueryType() {
        String queryType = (String) queryTypeCombo.getSelectedItem();

        // Show/hide panels based on query type
        boolean showLeague = FixturesNodeModel.QUERY_BY_LEAGUE.equals(queryType) ||
                            FixturesNodeModel.QUERY_BY_TEAM.equals(queryType);
        boolean showDate = FixturesNodeModel.QUERY_BY_DATE.equals(queryType);
        boolean showFixtureId = FixturesNodeModel.QUERY_BY_ID.equals(queryType);
        boolean showH2H = FixturesNodeModel.QUERY_H2H.equals(queryType);

        dateRangePanel.setVisible(showDate);
        fixtureIdPanel.setVisible(showFixtureId);
        h2hPanel.setVisible(showH2H);

        // Update league/season/team controls based on query type
        leagueCombo.setEnabled(showLeague || showDate || showH2H);
        // Season is required for league, team, date range, and H2H queries
        seasonCombo.setEnabled(showLeague || showDate || showH2H || FixturesNodeModel.QUERY_BY_TEAM.equals(queryType));
        // Enable team for league, team, date range, and H2H queries
        teamCombo.setEnabled(showLeague || showDate || showH2H || FixturesNodeModel.QUERY_BY_TEAM.equals(queryType));

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    @Override
    protected void loadAdditionalSettings(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {
        // Load fixtures-specific settings
        String queryType = settings.getString(FixturesNodeModel.CFGKEY_QUERY_TYPE,
                                              FixturesNodeModel.QUERY_BY_LEAGUE);
        String fixtureId = settings.getString(FixturesNodeModel.CFGKEY_FIXTURE_ID, "");
        String status = settings.getString(FixturesNodeModel.CFGKEY_STATUS, "");
        int team2Id = settings.getInt(FixturesNodeModel.CFGKEY_TEAM2_ID, -1);
        boolean includeEvents = settings.getBoolean(FixturesNodeModel.CFGKEY_INCLUDE_EVENTS, false);
        boolean includeLineups = settings.getBoolean(FixturesNodeModel.CFGKEY_INCLUDE_LINEUPS, false);
        boolean includeStatistics = settings.getBoolean(FixturesNodeModel.CFGKEY_INCLUDE_STATISTICS, false);
        boolean includePlayerStats = settings.getBoolean(FixturesNodeModel.CFGKEY_INCLUDE_PLAYER_STATS, false);

        // PRE-POPULATE from flow variables if available (from upstream Fixtures Selector)
        // This allows Fixtures node to inherit settings from Fixtures Selector
        prePopulateFromFlowVariables();

        // Set values in UI components
        queryTypeCombo.setSelectedItem(queryType);

        // Load date range panel settings
        dateRangePanel.loadSettingsFrom(settings, specs);

        fixtureIdField.setText(fixtureId);
        statusCombo.setSelectedItem(status);
        includeEventsCheck.setSelected(includeEvents);
        includeLineupsCheck.setSelected(includeLineups);
        includeStatisticsCheck.setSelected(includeStatistics);
        includePlayerStatsCheck.setSelected(includePlayerStats);

        // Populate and select team2 for H2H queries
        populateTeam2Combo();
        selectTeam2(team2Id);

        updateVisibilityForQueryType();
    }

    /**
     * Pre-populate dialog fields from flow variables if they exist.
     * This is called when node is connected to Fixtures Selector.
     */
    private void prePopulateFromFlowVariables() {
        try {
            // Check if flow variables exist (from upstream Fixtures Selector)
            Map<String, org.knime.core.node.workflow.FlowVariable> flowVars = getAvailableFlowVariables();

            if (flowVars.containsKey("fixtures_league_id")) {
                int leagueId = flowVars.get("fixtures_league_id").getIntValue();
                selectLeagueById(leagueId);
            }

            if (flowVars.containsKey("fixtures_season")) {
                int season = flowVars.get("fixtures_season").getIntValue();
                selectSeasonByValue(season);
            }

            if (flowVars.containsKey("fixtures_team_id")) {
                int teamId = flowVars.get("fixtures_team_id").getIntValue();
                if (teamId > 0) {
                    selectTeamById(teamId);
                }
            }

            // Show info message to user
            if (flowVars.containsKey("fixtures_query_type")) {
                String info = "Pre-populated from upstream Fixtures Selector";
                JOptionPane.showMessageDialog(getPanel(),
                    info,
                    "Settings Loaded",
                    JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception e) {
            // Flow variables not available or error reading them - use defaults
            // This is normal if not connected to Fixtures Selector
        }
    }

    /**
     * Select league in combo by ID.
     */
    private void selectLeagueById(int leagueId) {
        for (int i = 0; i < leagueCombo.getItemCount(); i++) {
            LeagueItem item = leagueCombo.getItemAt(i);
            if (item.id == leagueId) {
                leagueCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Select season in combo by value.
     */
    private void selectSeasonByValue(int season) {
        for (int i = 0; i < seasonCombo.getItemCount(); i++) {
            Integer item = seasonCombo.getItemAt(i);
            if (item != null && item == season) {
                seasonCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Select team in combo by ID.
     */
    private void selectTeamById(int teamId) {
        for (int i = 0; i < teamCombo.getItemCount(); i++) {
            TeamItem item = teamCombo.getItemAt(i);
            if (item != null && item.id == teamId) {
                teamCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        // Save fixtures-specific settings
        settings.addString(FixturesNodeModel.CFGKEY_QUERY_TYPE,
                          (String) queryTypeCombo.getSelectedItem());

        // Save date range panel settings
        dateRangePanel.saveSettingsTo(settings);

        settings.addString(FixturesNodeModel.CFGKEY_FIXTURE_ID, fixtureIdField.getText());
        settings.addString(FixturesNodeModel.CFGKEY_STATUS,
                          (String) statusCombo.getSelectedItem());

        // Save team2 for H2H queries
        TeamItem selectedTeam2 = (TeamItem) team2Combo.getSelectedItem();
        settings.addInt(FixturesNodeModel.CFGKEY_TEAM2_ID,
                       selectedTeam2 != null ? selectedTeam2.id : -1);

        settings.addBoolean(FixturesNodeModel.CFGKEY_INCLUDE_EVENTS, includeEventsCheck.isSelected());
        settings.addBoolean(FixturesNodeModel.CFGKEY_INCLUDE_LINEUPS, includeLineupsCheck.isSelected());
        settings.addBoolean(FixturesNodeModel.CFGKEY_INCLUDE_STATISTICS, includeStatisticsCheck.isSelected());
        settings.addBoolean(FixturesNodeModel.CFGKEY_INCLUDE_PLAYER_STATS, includePlayerStatsCheck.isSelected());
    }
}
