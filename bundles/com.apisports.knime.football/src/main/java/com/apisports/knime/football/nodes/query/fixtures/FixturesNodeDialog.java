package com.apisports.knime.football.nodes.query.fixtures;

import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeDialog;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog for Fixtures query node.
 */
public class FixturesNodeDialog extends AbstractFootballQueryNodeDialog {

    private JComboBox<String> queryTypeCombo;
    private JTextField fromDateField;
    private JTextField toDateField;
    private JTextField fixtureIdField;
    private JComboBox<String> statusCombo;
    private JCheckBox includeEventsCheck;
    private JCheckBox includeLineupsCheck;
    private JCheckBox includeStatisticsCheck;
    private JComboBox<TeamItem> team2Combo;

    // Panels that show/hide based on query type
    private JPanel dateRangePanel;
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

        // Date range panel (for date queries)
        dateRangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dateRangePanel.add(new JLabel("From Date (YYYY-MM-DD):"));
        fromDateField = new JTextField(12);
        dateRangePanel.add(fromDateField);
        dateRangePanel.add(new JLabel("  To Date:"));
        toDateField = new JTextField(12);
        dateRangePanel.add(toDateField);
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
        includePanel.add(includeEventsCheck);
        includePanel.add(includeLineupsCheck);
        includePanel.add(includeStatisticsCheck);
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
        String fromDate = settings.getString(FixturesNodeModel.CFGKEY_FROM_DATE, "");
        String toDate = settings.getString(FixturesNodeModel.CFGKEY_TO_DATE, "");
        String fixtureId = settings.getString(FixturesNodeModel.CFGKEY_FIXTURE_ID, "");
        String status = settings.getString(FixturesNodeModel.CFGKEY_STATUS, "");
        int team2Id = settings.getInt(FixturesNodeModel.CFGKEY_TEAM2_ID, -1);
        boolean includeEvents = settings.getBoolean(FixturesNodeModel.CFGKEY_INCLUDE_EVENTS, false);
        boolean includeLineups = settings.getBoolean(FixturesNodeModel.CFGKEY_INCLUDE_LINEUPS, false);
        boolean includeStatistics = settings.getBoolean(FixturesNodeModel.CFGKEY_INCLUDE_STATISTICS, false);

        // Set values in UI components
        queryTypeCombo.setSelectedItem(queryType);
        fromDateField.setText(fromDate);
        toDateField.setText(toDate);
        fixtureIdField.setText(fixtureId);
        statusCombo.setSelectedItem(status);
        includeEventsCheck.setSelected(includeEvents);
        includeLineupsCheck.setSelected(includeLineups);
        includeStatisticsCheck.setSelected(includeStatistics);

        // Populate and select team2 for H2H queries
        populateTeam2Combo();
        selectTeam2(team2Id);

        updateVisibilityForQueryType();
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        // Save fixtures-specific settings
        settings.addString(FixturesNodeModel.CFGKEY_QUERY_TYPE,
                          (String) queryTypeCombo.getSelectedItem());
        settings.addString(FixturesNodeModel.CFGKEY_FROM_DATE, fromDateField.getText());
        settings.addString(FixturesNodeModel.CFGKEY_TO_DATE, toDateField.getText());
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
    }
}
