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

package com.apisports.knime.football.nodes.query;

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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Abstract base dialog for Football API query nodes.
 * Provides common UI components for selecting league, season, and team.
 */
public abstract class AbstractFootballQueryNodeDialog extends NodeDialogPane {

    // Common UI components
    protected JComboBox<LeagueItem> leagueCombo;
    protected JComboBox<Integer> seasonCombo;
    protected JComboBox<TeamItem> teamCombo;

    // Reference data loaded from database
    protected List<ReferenceData.League> allLeagues;
    protected List<ReferenceData.Season> allSeasons;
    protected List<ReferenceData.Team> allTeams;

    // Main panel for subclasses to add their specific components
    protected JPanel mainPanel;

    /**
     * Helper class to display league name with country in dropdown.
     */
    public static class LeagueItem {
        public final int id;
        public final String name;
        public final String country;

        public LeagueItem(int id, String name, String country) {
            this.id = id;
            this.name = name;
            this.country = country;
        }

        @Override
        public String toString() {
            return name + " (" + country + ")";
        }
    }

    /**
     * Helper class to display team name in dropdown.
     */
    public static class TeamItem {
        public final int id;
        public final String name;

        public TeamItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    protected AbstractFootballQueryNodeDialog() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Create common UI components
        createCommonComponents();

        // Subclasses will add their specific components
        addTab("Configuration", new JScrollPane(mainPanel));
    }

    /**
     * Create common UI components (league, season, team selection).
     */
    private void createCommonComponents() {
        // League selection
        JPanel leaguePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leaguePanel.add(new JLabel("League:"));
        leagueCombo = new JComboBox<>();
        leagueCombo.setPreferredSize(new Dimension(300, 25));
        leagueCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onLeagueChanged();
            }
        });
        leaguePanel.add(leagueCombo);
        mainPanel.add(leaguePanel);

        // Season selection
        JPanel seasonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        seasonPanel.add(new JLabel("Season:"));
        seasonCombo = new JComboBox<>();
        seasonCombo.setPreferredSize(new Dimension(150, 25));
        seasonCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSeasonChanged();
            }
        });
        seasonPanel.add(seasonCombo);
        mainPanel.add(seasonPanel);

        // Team selection (optional for most endpoints)
        JPanel teamPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        teamPanel.add(new JLabel("Team:"));
        teamCombo = new JComboBox<>();
        teamCombo.setPreferredSize(new Dimension(300, 25));
        teamPanel.add(teamCombo);
        mainPanel.add(teamPanel);

        mainPanel.add(Box.createVerticalStrut(10)); // Spacer
    }

    /**
     * Called when league selection changes.
     * Updates season and team dropdowns to show only items for selected league.
     */
    protected void onLeagueChanged() {
        LeagueItem selectedLeague = (LeagueItem) leagueCombo.getSelectedItem();
        if (selectedLeague == null) {
            return;
        }

        updateSeasonCombo(selectedLeague.id);
        updateTeamCombo(selectedLeague.id);
    }

    /**
     * Called when season selection changes.
     * Subclasses can override to add custom behavior.
     */
    protected void onSeasonChanged() {
        // Default: do nothing
        // Subclasses can override if needed
    }

    /**
     * Update season dropdown to show seasons for selected league.
     */
    private void updateSeasonCombo(int leagueId) {
        seasonCombo.removeAllItems();

        if (allSeasons != null) {
            for (ReferenceData.Season season : allSeasons) {
                if (season.getLeagueId() == leagueId) {
                    seasonCombo.addItem(season.getYear());
                }
            }
        }
    }

    /**
     * Update team dropdown to show teams for selected league, or all teams if no league.
     */
    private void updateTeamCombo(int leagueId) {
        teamCombo.removeAllItems();
        teamCombo.addItem(new TeamItem(-1, "-- All Teams --"));

        if (allTeams != null) {
            if (leagueId > 0) {
                // Filter teams by selected league
                for (ReferenceData.Team team : allTeams) {
                    if (team.getLeagueIds().contains(leagueId)) {
                        teamCombo.addItem(new TeamItem(team.getId(), team.getName()));
                    }
                }
            } else {
                // No league selected - show all teams
                for (ReferenceData.Team team : allTeams) {
                    teamCombo.addItem(new TeamItem(team.getId(), team.getName()));
                }
            }
        }
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {

        // Get reference data from input port
        if (specs.length < 2 || !(specs[1] instanceof ReferenceDataPortObjectSpec)) {
            throw new NotConfigurableException("Reference data port not connected");
        }

        ReferenceDataPortObjectSpec refSpec = (ReferenceDataPortObjectSpec) specs[1];
        String dbPath = refSpec.getDbPath();

        // Load reference data from database
        loadReferenceData(dbPath);

        // Populate league dropdown
        populateLeagueCombo();

        // Load saved settings
        int savedLeagueId = settings.getInt(AbstractFootballQueryNodeModel.CFGKEY_LEAGUE_ID, -1);
        int savedSeason = settings.getInt(AbstractFootballQueryNodeModel.CFGKEY_SEASON, -1);
        int savedTeamId = settings.getInt(AbstractFootballQueryNodeModel.CFGKEY_TEAM_ID, -1);

        // Initialize team combo with all teams (will be filtered when league is selected)
        updateTeamCombo(savedLeagueId);

        // Restore selections
        selectLeague(savedLeagueId);
        selectSeason(savedSeason);
        selectTeam(savedTeamId);

        // Let subclasses load their specific settings
        loadAdditionalSettings(settings, specs);
    }

    /**
     * Load reference data from SQLite database.
     */
    private void loadReferenceData(String dbPath) throws NotConfigurableException {
        try (ReferenceDAO dao = new ReferenceDAO(dbPath)) {
            allLeagues = dao.getAllLeagues();
            allSeasons = dao.getAllSeasons();
            allTeams = dao.getAllTeams();
        } catch (Exception e) {
            throw new NotConfigurableException("Failed to load reference data: " + e.getMessage(), e);
        }
    }

    /**
     * Populate league dropdown with all available leagues.
     */
    private void populateLeagueCombo() {
        leagueCombo.removeAllItems();

        // Add placeholder for "no league selected" - shows all teams
        leagueCombo.addItem(new LeagueItem(-1, "-- All Leagues --", ""));

        if (allLeagues != null) {
            for (ReferenceData.League league : allLeagues) {
                leagueCombo.addItem(new LeagueItem(
                    league.getId(),
                    league.getName(),
                    league.getCountryName()
                ));
            }
        }
    }

    /**
     * Select league by ID in the combo box.
     */
    private void selectLeague(int leagueId) {
        for (int i = 0; i < leagueCombo.getItemCount(); i++) {
            LeagueItem item = leagueCombo.getItemAt(i);
            if (item.id == leagueId) {
                leagueCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Select season in the combo box.
     */
    private void selectSeason(int season) {
        for (int i = 0; i < seasonCombo.getItemCount(); i++) {
            Integer item = seasonCombo.getItemAt(i);
            if (item != null && item == season) {
                seasonCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Select team by ID in the combo box.
     */
    private void selectTeam(int teamId) {
        for (int i = 0; i < teamCombo.getItemCount(); i++) {
            TeamItem item = teamCombo.getItemAt(i);
            if (item.id == teamId) {
                teamCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        // Save common settings
        LeagueItem selectedLeague = (LeagueItem) leagueCombo.getSelectedItem();
        Integer selectedSeason = (Integer) seasonCombo.getSelectedItem();
        TeamItem selectedTeam = (TeamItem) teamCombo.getSelectedItem();

        settings.addInt(AbstractFootballQueryNodeModel.CFGKEY_LEAGUE_ID,
                       selectedLeague != null ? selectedLeague.id : -1);
        settings.addInt(AbstractFootballQueryNodeModel.CFGKEY_SEASON,
                       selectedSeason != null ? selectedSeason : -1);
        settings.addInt(AbstractFootballQueryNodeModel.CFGKEY_TEAM_ID,
                       selectedTeam != null ? selectedTeam.id : -1);

        // Let subclasses save their specific settings
        saveAdditionalSettings(settings);
    }

    /**
     * Subclasses override to load their specific settings.
     */
    protected abstract void loadAdditionalSettings(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException;

    /**
     * Subclasses override to save their specific settings.
     */
    protected abstract void saveAdditionalSettings(NodeSettingsWO settings)
            throws InvalidSettingsException;
}
