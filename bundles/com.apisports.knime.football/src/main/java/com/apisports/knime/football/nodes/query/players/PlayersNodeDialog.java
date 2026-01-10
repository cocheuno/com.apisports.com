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

package com.apisports.knime.football.nodes.query.players;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dialog for Players query node.
 */
public class PlayersNodeDialog extends AbstractFootballQueryNodeDialog {

    private JComboBox<String> queryTypeCombo;
    private JTextField playerNameField;
    private JTextField playerIdField;
    private JPanel nameSearchPanel;
    private JPanel idSearchPanel;

    // Multi-selection team list (replaces inherited teamCombo for main team selection)
    private JList<TeamItem> teamList;
    private DefaultListModel<TeamItem> teamListModel;
    private JScrollPane teamScrollPane;

    public PlayersNodeDialog() {
        super();
        // Hide the inherited single-selection teamCombo from parent class
        teamCombo.setVisible(false);

        // Create multi-selection team list to replace the combo
        createTeamList();

        addPlayersSpecificComponents();
    }

    /**
     * Create multi-selection team list to replace the inherited single-selection teamCombo.
     */
    private void createTeamList() {
        teamListModel = new DefaultListModel<>();
        teamList = new JList<>(teamListModel);
        teamList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        teamList.setVisibleRowCount(6);

        teamScrollPane = new JScrollPane(teamList);
        teamScrollPane.setPreferredSize(new Dimension(300, 120));

        // Create panel for team list and add it after the parent's team panel (which is now hidden)
        JPanel teamListPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        teamListPanel.add(new JLabel("Teams:"));
        teamListPanel.add(teamScrollPane);
        teamListPanel.add(new JLabel("(Select one or more)"));

        // Add after the existing (but hidden) team panel
        // The parent class adds: League, Season, Team panels
        // We insert our team list panel at index 2 (after League and Season)
        mainPanel.add(teamListPanel, 2);
    }

    private void addPlayersSpecificComponents() {
        // Add separator
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        mainPanel.add(Box.createVerticalStrut(10));

        // Query type selection
        JPanel queryTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        queryTypePanel.add(new JLabel("Query Type:"));
        queryTypeCombo = new JComboBox<>(new String[]{
            PlayersNodeModel.QUERY_TOP_SCORERS,
            PlayersNodeModel.QUERY_TOP_ASSISTS,
            PlayersNodeModel.QUERY_TOP_YELLOW_CARDS,
            PlayersNodeModel.QUERY_TOP_RED_CARDS,
            PlayersNodeModel.QUERY_BY_TEAM,
            PlayersNodeModel.QUERY_BY_NAME,
            PlayersNodeModel.QUERY_BY_ID
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

        // Player name search panel
        nameSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nameSearchPanel.add(new JLabel("Player Name:"));
        playerNameField = new JTextField(25);
        nameSearchPanel.add(playerNameField);
        mainPanel.add(nameSearchPanel);

        // Player ID search panel
        idSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        idSearchPanel.add(new JLabel("Player ID:"));
        playerIdField = new JTextField(15);
        idSearchPanel.add(playerIdField);
        mainPanel.add(idSearchPanel);

        // Add help text
        JTextArea helpText = new JTextArea(
            "Query Types:\n" +
            "• Top Scorers/Assists/Cards: Get league-wide leaders (no team filter)\n" +
            "• Players by Team: Get all players for a specific team\n" +
            "• Search by Name: Find players by name (supports team filter)\n" +
            "• By Player ID: Get detailed stats for specific player"
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
        // Call parent to update season combo (skip parent's team combo update since it's hidden)
        LeagueItem selectedLeague = (LeagueItem) leagueCombo.getSelectedItem();
        if (selectedLeague == null) {
            return;
        }

        // Update season combo (from parent logic)
        if (allSeasons != null) {
            seasonCombo.removeAllItems();
            for (com.apisports.knime.port.ReferenceData.Season season : allSeasons) {
                if (season.getLeagueId() == selectedLeague.id) {
                    seasonCombo.addItem(season.getYear());
                }
            }
        }

        // Populate team list (replaces parent's teamCombo logic)
        populateTeamList();
    }

    /**
     * Populate team list with teams from selected league.
     */
    private void populateTeamList() {
        LeagueItem selectedLeague = (LeagueItem) leagueCombo.getSelectedItem();
        if (selectedLeague == null) {
            return;
        }

        teamListModel.clear();

        if (allTeams != null) {
            for (com.apisports.knime.port.ReferenceData.Team team : allTeams) {
                if (team.getLeagueIds().contains(selectedLeague.id)) {
                    teamListModel.addElement(new TeamItem(team.getId(), team.getName()));
                }
            }
        }
    }

    /**
     * Select team in list by ID.
     */
    private void selectTeamById(int teamId) {
        for (int i = 0; i < teamListModel.getSize(); i++) {
            TeamItem item = teamListModel.getElementAt(i);
            if (item != null && item.id == teamId) {
                teamList.addSelectionInterval(i, i);
                break;
            }
        }
    }

    /**
     * Select multiple teams in list by IDs.
     */
    private void selectTeamsByIds(int[] teamIds) {
        teamList.clearSelection();
        if (teamIds == null || teamIds.length == 0) {
            return;
        }

        for (int teamId : teamIds) {
            if (teamId > 0) {
                selectTeamById(teamId);
            }
        }

        // Ensure first selected item is visible
        if (teamList.getSelectedIndex() >= 0) {
            teamList.ensureIndexIsVisible(teamList.getSelectedIndex());
        }
    }

    private void updateVisibilityForQueryType() {
        String queryType = (String) queryTypeCombo.getSelectedItem();

        boolean showNameSearch = PlayersNodeModel.QUERY_BY_NAME.equals(queryType);
        boolean showIdSearch = PlayersNodeModel.QUERY_BY_ID.equals(queryType);
        boolean showLeagueSeason = !PlayersNodeModel.QUERY_BY_ID.equals(queryType)
                                    && !PlayersNodeModel.QUERY_BY_TEAM.equals(queryType);

        // Only enable team combo for queries that support team filtering
        boolean enableTeamCombo = PlayersNodeModel.QUERY_BY_TEAM.equals(queryType)
                                || PlayersNodeModel.QUERY_BY_NAME.equals(queryType);

        nameSearchPanel.setVisible(showNameSearch);
        idSearchPanel.setVisible(showIdSearch);
        leagueCombo.setEnabled(showLeagueSeason);
        seasonCombo.setEnabled(true); // Always needed
        teamList.setEnabled(enableTeamCombo);

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    @Override
    protected void loadAdditionalSettings(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {
        String queryType = settings.getString(PlayersNodeModel.CFGKEY_QUERY_TYPE,
                                              PlayersNodeModel.QUERY_TOP_SCORERS);
        String playerName = settings.getString(PlayersNodeModel.CFGKEY_PLAYER_NAME, "");
        String playerId = settings.getString(PlayersNodeModel.CFGKEY_PLAYER_ID, "");

        // Load team IDs (multi-selection support)
        int[] teamIds = settings.getIntArray(PlayersNodeModel.CFGKEY_TEAM_IDS, new int[]{});

        // PRE-POPULATE from flow variables if available (from upstream Players Selector)
        // This allows Players node to inherit settings from Players Selector
        prePopulateFromFlowVariables();

        queryTypeCombo.setSelectedItem(queryType);
        playerNameField.setText(playerName);
        playerIdField.setText(playerId);

        // Populate and select teams in list
        populateTeamList();
        selectTeamsByIds(teamIds);

        updateVisibilityForQueryType();
    }

    /**
     * Pre-populate dialog fields from flow variables if they exist.
     * This is called when node is connected to Players Selector.
     */
    private void prePopulateFromFlowVariables() {
        try {
            // Check if flow variables exist (from upstream Players Selector)
            Map<String, org.knime.core.node.workflow.FlowVariable> flowVars = getAvailableFlowVariables();

            if (flowVars.containsKey("players_league_id")) {
                int leagueId = flowVars.get("players_league_id").getIntValue();
                selectLeagueById(leagueId);
            }

            if (flowVars.containsKey("players_season")) {
                int season = flowVars.get("players_season").getIntValue();
                selectSeasonByValue(season);
            }

            if (flowVars.containsKey("players_team_id")) {
                int teamId = flowVars.get("players_team_id").getIntValue();
                if (teamId > 0) {
                    // Select team from Players Selector - highlight it in the list
                    selectTeamsByIds(new int[]{teamId});
                }
            }

            // Show info message to user
            if (flowVars.containsKey("players_query_type")) {
                String info = "Pre-populated from upstream Players Selector";
                JOptionPane.showMessageDialog(getPanel(),
                    info,
                    "Settings Loaded",
                    JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception e) {
            // Flow variables not available or error reading them - use defaults
            // This is normal if not connected to Players Selector
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

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        settings.addString(PlayersNodeModel.CFGKEY_QUERY_TYPE,
                          (String) queryTypeCombo.getSelectedItem());
        settings.addString(PlayersNodeModel.CFGKEY_PLAYER_NAME, playerNameField.getText());
        settings.addString(PlayersNodeModel.CFGKEY_PLAYER_ID, playerIdField.getText());

        // Save selected teams (multi-selection support)
        List<TeamItem> selectedTeams = teamList.getSelectedValuesList();
        int[] teamIds = new int[selectedTeams.size()];
        for (int i = 0; i < selectedTeams.size(); i++) {
            teamIds[i] = selectedTeams.get(i).id;
        }
        settings.addIntArray(PlayersNodeModel.CFGKEY_TEAM_IDS, teamIds);

        // Also save first team ID for backwards compatibility with parent class
        int firstTeamId = teamIds.length > 0 ? teamIds[0] : -1;
        settings.addInt("teamId", firstTeamId);  // Parent class uses "teamId" key
    }
}
