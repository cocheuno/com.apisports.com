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

        // H2H panel (for head-to-head queries)
        h2hPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        h2hPanel.add(new JLabel("Team 1 ID:"));
        JTextField team1Field = new JTextField(10);
        h2hPanel.add(team1Field);
        h2hPanel.add(new JLabel("  Team 2 ID:"));
        JTextField team2Field = new JTextField(10);
        h2hPanel.add(team2Field);
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
            "• By Date Range: Get fixtures within a date range\n" +
            "• By Team: Get all fixtures for a specific team in a season\n" +
            "• By Fixture ID: Get a specific fixture by its ID\n" +
            "• Live Fixtures: Get all currently live fixtures\n" +
            "• Head to Head: Compare two teams' past encounters"
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
        leagueCombo.setEnabled(showLeague || showDate);
        seasonCombo.setEnabled(showLeague || FixturesNodeModel.QUERY_BY_TEAM.equals(queryType));
        teamCombo.setEnabled(FixturesNodeModel.QUERY_BY_TEAM.equals(queryType) || showLeague);

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
        settings.addBoolean(FixturesNodeModel.CFGKEY_INCLUDE_EVENTS, includeEventsCheck.isSelected());
        settings.addBoolean(FixturesNodeModel.CFGKEY_INCLUDE_LINEUPS, includeLineupsCheck.isSelected());
        settings.addBoolean(FixturesNodeModel.CFGKEY_INCLUDE_STATISTICS, includeStatisticsCheck.isSelected());
    }
}
