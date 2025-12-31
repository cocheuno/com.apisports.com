/*
 * Copyright 2025 Carone Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 * Dialog for Fixtures Selector node.
 * Lightweight selector for browsing and filtering fixtures without expensive API calls.
 */
public class FixturesSelectorNodeDialog extends AbstractFootballQueryNodeDialog {

    private JComboBox<String> queryTypeCombo;
    private DateRangePanel dateRangePanel;  // NEW: Replaces simple text fields
    private JTextField fixtureIdField;
    private JComboBox<String> statusCombo;
    private JComboBox<TeamItem> team2Combo;

    // Panels that show/hide based on query type
    private JPanel fixtureIdPanel;
    private JPanel h2hPanel;

    public FixturesSelectorNodeDialog() {
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
            FixturesSelectorNodeModel.QUERY_BY_LEAGUE,
            FixturesSelectorNodeModel.QUERY_BY_DATE,
            FixturesSelectorNodeModel.QUERY_BY_TEAM,
            FixturesSelectorNodeModel.QUERY_BY_ID,
            FixturesSelectorNodeModel.QUERY_LIVE,
            FixturesSelectorNodeModel.QUERY_H2H
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

        // Date range panel - NEW: Use DateRangePanel component
        // Note: Incremental mode disabled for selector (selectors are lightweight discovery)
        dateRangePanel = new DateRangePanel("lastFixtureQuery", false);
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

        // Add help text
        JTextArea helpText = new JTextArea(
            "Fixtures Selector - Lightweight fixture browsing and selection\n\n" +
            "Query Types:\n" +
            "• By League/Season: Get all fixtures for a league and season\n" +
            "• By Date Range: Get fixtures within a date range (requires season)\n" +
            "• By Team: Get all fixtures for a specific team in a season\n" +
            "• By Fixture ID: Get a specific fixture by its ID\n" +
            "• Live Fixtures: Get all currently live fixtures\n" +
            "• Head to Head: Compare two teams in a specific season\n\n" +
            "Note: This node returns basic fixture information only (15 columns).\n" +
            "For detailed statistics, lineups, and player stats, filter the output\n" +
            "and feed it into the Fixtures Details node.\n\n" +
            "Dates accept YYYY-MM-DD or YYYY/MM/DD format"
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
        boolean showLeague = FixturesSelectorNodeModel.QUERY_BY_LEAGUE.equals(queryType) ||
                            FixturesSelectorNodeModel.QUERY_BY_TEAM.equals(queryType);
        boolean showDate = FixturesSelectorNodeModel.QUERY_BY_DATE.equals(queryType);
        boolean showFixtureId = FixturesSelectorNodeModel.QUERY_BY_ID.equals(queryType);
        boolean showH2H = FixturesSelectorNodeModel.QUERY_H2H.equals(queryType);

        dateRangePanel.setVisible(showDate);
        fixtureIdPanel.setVisible(showFixtureId);
        h2hPanel.setVisible(showH2H);

        // Update league/season/team controls based on query type
        leagueCombo.setEnabled(showLeague || showDate || showH2H);
        // Season is required for league, team, date range, and H2H queries
        seasonCombo.setEnabled(showLeague || showDate || showH2H || FixturesSelectorNodeModel.QUERY_BY_TEAM.equals(queryType));
        // Enable team for league, team, date range, and H2H queries
        teamCombo.setEnabled(showLeague || showDate || showH2H || FixturesSelectorNodeModel.QUERY_BY_TEAM.equals(queryType));

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    @Override
    protected void loadAdditionalSettings(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {
        // Load fixtures-specific settings
        String queryType = settings.getString(FixturesSelectorNodeModel.CFGKEY_QUERY_TYPE,
                                              FixturesSelectorNodeModel.QUERY_BY_LEAGUE);
        String fixtureId = settings.getString(FixturesSelectorNodeModel.CFGKEY_FIXTURE_ID, "");
        String status = settings.getString(FixturesSelectorNodeModel.CFGKEY_STATUS, "");
        int team2Id = settings.getInt(FixturesSelectorNodeModel.CFGKEY_TEAM2_ID, -1);

        // Set values in UI components
        queryTypeCombo.setSelectedItem(queryType);

        // Load date range panel settings
        dateRangePanel.loadSettingsFrom(settings, specs);

        fixtureIdField.setText(fixtureId);
        statusCombo.setSelectedItem(status);

        // Populate and select team2 for H2H queries
        populateTeam2Combo();
        selectTeam2(team2Id);

        updateVisibilityForQueryType();
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        // Save fixtures-specific settings
        settings.addString(FixturesSelectorNodeModel.CFGKEY_QUERY_TYPE,
                          (String) queryTypeCombo.getSelectedItem());

        // Save date range panel settings
        dateRangePanel.saveSettingsTo(settings);

        settings.addString(FixturesSelectorNodeModel.CFGKEY_FIXTURE_ID, fixtureIdField.getText());
        settings.addString(FixturesSelectorNodeModel.CFGKEY_STATUS,
                          (String) statusCombo.getSelectedItem());

        // Save team2 for H2H queries
        TeamItem selectedTeam2 = (TeamItem) team2Combo.getSelectedItem();
        settings.addInt(FixturesSelectorNodeModel.CFGKEY_TEAM2_ID,
                       selectedTeam2 != null ? selectedTeam2.id : -1);
    }
}
