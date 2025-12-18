package com.apisports.knime.football.nodes.teamstats;

import com.apisports.knime.port.ReferenceData;
import com.apisports.knime.port.ReferenceData.League;
import com.apisports.knime.port.ReferenceData.Team;
import com.apisports.knime.port.ReferenceDataPortObject;
import com.apisports.knime.port.ReferenceDataPortObjectSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for Team Statistics query node.
 * Provides cascading dropdowns based on reference data.
 */
public class TeamStatisticsNodeDialog extends NodeDialogPane {

    private JComboBox<LeagueItem> m_leagueCombo;
    private JComboBox<Integer> m_seasonCombo;
    private JComboBox<TeamItem> m_teamCombo;

    private ReferenceData m_referenceData;
    private List<League> m_allLeagues = new ArrayList<>();
    private List<Team> m_allTeams = new ArrayList<>();

    public TeamStatisticsNodeDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // League dropdown
        panel.add(new JLabel("League:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        m_leagueCombo = new JComboBox<>();
        m_leagueCombo.addActionListener(e -> onLeagueChanged());
        panel.add(m_leagueCombo, gbc);

        // Season dropdown
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Season:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        m_seasonCombo = new JComboBox<>(new Integer[]{2024, 2023, 2022, 2021, 2020});
        panel.add(m_seasonCombo, gbc);

        // Team dropdown
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Team:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        m_teamCombo = new JComboBox<>();
        panel.add(m_teamCombo, gbc);

        addTab("Team Statistics", panel);
    }

    /**
     * Called when league selection changes - update team dropdown.
     */
    private void onLeagueChanged() {
        LeagueItem selectedLeague = (LeagueItem) m_leagueCombo.getSelectedItem();
        if (selectedLeague == null || m_referenceData == null) {
            return;
        }

        // Filter teams for this league
        List<Team> teamsForLeague = m_referenceData.getTeamsForLeague(selectedLeague.id);

        // Update team combo
        m_teamCombo.removeAllItems();
        for (Team team : teamsForLeague) {
            m_teamCombo.addItem(new TeamItem(team.getId(), team.getName()));
        }
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {

        // Get reference data from port 0
        if (specs[0] == null || !(specs[0] instanceof ReferenceDataPortObjectSpec)) {
            throw new NotConfigurableException(
                "Reference data port not connected. Please connect Reference Data Loader node to port 0.");
        }

        // Note: We can't access the actual ReferenceData from PortObjectSpec
        // We need to handle this differently - store reference data in the node model
        // or pass it through a different mechanism.
        // For now, we'll populate with default options and let the user configure.

        // TODO: Access actual reference data
        // As a workaround, populate with some default leagues
        populateDefaultLeagues();

        // Load saved settings
        int leagueId = settings.getInt(TeamStatisticsNodeModel.CFGKEY_LEAGUE, 39);
        int season = settings.getInt(TeamStatisticsNodeModel.CFGKEY_SEASON, 2024);
        int teamId = settings.getInt(TeamStatisticsNodeModel.CFGKEY_TEAM, 42);

        // Set selections
        selectLeagueById(leagueId);
        m_seasonCombo.setSelectedItem(season);
        selectTeamById(teamId);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        LeagueItem selectedLeague = (LeagueItem) m_leagueCombo.getSelectedItem();
        if (selectedLeague == null) {
            throw new InvalidSettingsException("Please select a league");
        }

        TeamItem selectedTeam = (TeamItem) m_teamCombo.getSelectedItem();
        if (selectedTeam == null) {
            throw new InvalidSettingsException("Please select a team");
        }

        Integer selectedSeason = (Integer) m_seasonCombo.getSelectedItem();
        if (selectedSeason == null) {
            throw new InvalidSettingsException("Please select a season");
        }

        settings.addInt(TeamStatisticsNodeModel.CFGKEY_LEAGUE, selectedLeague.id);
        settings.addInt(TeamStatisticsNodeModel.CFGKEY_SEASON, selectedSeason);
        settings.addInt(TeamStatisticsNodeModel.CFGKEY_TEAM, selectedTeam.id);
    }

    /**
     * Populate with default major leagues (temporary workaround).
     */
    private void populateDefaultLeagues() {
        m_leagueCombo.removeAllItems();
        m_leagueCombo.addItem(new LeagueItem(39, "Premier League"));
        m_leagueCombo.addItem(new LeagueItem(140, "La Liga"));
        m_leagueCombo.addItem(new LeagueItem(78, "Bundesliga"));
        m_leagueCombo.addItem(new LeagueItem(135, "Serie A"));
        m_leagueCombo.addItem(new LeagueItem(61, "Ligue 1"));

        // Populate with some default teams for Premier League
        m_teamCombo.removeAllItems();
        m_teamCombo.addItem(new TeamItem(42, "Arsenal"));
        m_teamCombo.addItem(new TeamItem(33, "Manchester United"));
        m_teamCombo.addItem(new TeamItem(40, "Liverpool"));
        m_teamCombo.addItem(new TeamItem(50, "Manchester City"));
        m_teamCombo.addItem(new TeamItem(49, "Chelsea"));
    }

    private void selectLeagueById(int leagueId) {
        for (int i = 0; i < m_leagueCombo.getItemCount(); i++) {
            if (m_leagueCombo.getItemAt(i).id == leagueId) {
                m_leagueCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectTeamById(int teamId) {
        for (int i = 0; i < m_teamCombo.getItemCount(); i++) {
            if (m_teamCombo.getItemAt(i).id == teamId) {
                m_teamCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Helper class for league combo items.
     */
    private static class LeagueItem {
        final int id;
        final String name;

        LeagueItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Helper class for team combo items.
     */
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
