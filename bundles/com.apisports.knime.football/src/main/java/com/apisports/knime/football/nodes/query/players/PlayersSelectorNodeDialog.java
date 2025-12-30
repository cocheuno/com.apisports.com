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

/**
 * Dialog for Players Selector node.
 * Lightweight selector for browsing and filtering players without detailed statistics.
 */
public class PlayersSelectorNodeDialog extends AbstractFootballQueryNodeDialog {

    private JComboBox<String> queryTypeCombo;
    private JTextField playerNameField;
    private JTextField playerIdField;
    private JPanel nameSearchPanel;
    private JPanel idSearchPanel;

    // Multi-selection team list (replaces inherited teamCombo for main team selection)
    private JList<TeamItem> teamList;
    private DefaultListModel<TeamItem> teamListModel;
    private JScrollPane teamScrollPane;

    public PlayersSelectorNodeDialog() {
        super();
        // Hide the inherited single-selection teamCombo from parent class
        teamCombo.setVisible(false);
        teamOptionalCheckbox.setVisible(false);

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
            PlayersSelectorNodeModel.QUERY_BY_TEAM,
            PlayersSelectorNodeModel.QUERY_BY_NAME,
            PlayersSelectorNodeModel.QUERY_BY_ID
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
            "Players Selector - Lightweight player browsing and selection\n\n" +
            "Query Types:\n" +
            "• Players by Team: Get all players for a specific team\n" +
            "• Search by Name: Find players by name (supports team filter)\n" +
            "• By Player ID: Get specific player by ID\n\n" +
            "Note: This node returns basic player information only (9 columns).\n" +
            "For detailed match-by-match statistics, filter the output and\n" +
            "feed it into the Player Stats node."
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

        boolean showNameSearch = PlayersSelectorNodeModel.QUERY_BY_NAME.equals(queryType);
        boolean showIdSearch = PlayersSelectorNodeModel.QUERY_BY_ID.equals(queryType);

        // Only enable team combo for queries that support team filtering
        boolean enableTeamCombo = PlayersSelectorNodeModel.QUERY_BY_TEAM.equals(queryType)
                                || PlayersSelectorNodeModel.QUERY_BY_NAME.equals(queryType);

        nameSearchPanel.setVisible(showNameSearch);
        idSearchPanel.setVisible(showIdSearch);
        leagueCombo.setEnabled(true); // Always useful for filtering
        seasonCombo.setEnabled(true); // Always needed
        teamList.setEnabled(enableTeamCombo);

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    @Override
    protected void loadAdditionalSettings(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {
        String queryType = settings.getString(PlayersSelectorNodeModel.CFGKEY_QUERY_TYPE,
                                              PlayersSelectorNodeModel.QUERY_BY_TEAM);
        String playerName = settings.getString(PlayersSelectorNodeModel.CFGKEY_PLAYER_NAME, "");
        String playerId = settings.getString(PlayersSelectorNodeModel.CFGKEY_PLAYER_ID, "");

        // Load team IDs (multi-selection support)
        int[] teamIds = settings.getIntArray(PlayersSelectorNodeModel.CFGKEY_TEAM_IDS, new int[]{});

        queryTypeCombo.setSelectedItem(queryType);
        playerNameField.setText(playerName);
        playerIdField.setText(playerId);

        // Populate and select teams in list
        populateTeamList();
        selectTeamsByIds(teamIds);

        updateVisibilityForQueryType();
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        settings.addString(PlayersSelectorNodeModel.CFGKEY_QUERY_TYPE,
                          (String) queryTypeCombo.getSelectedItem());
        settings.addString(PlayersSelectorNodeModel.CFGKEY_PLAYER_NAME, playerNameField.getText());
        settings.addString(PlayersSelectorNodeModel.CFGKEY_PLAYER_ID, playerIdField.getText());

        // Save selected teams (multi-selection support)
        List<TeamItem> selectedTeams = teamList.getSelectedValuesList();
        int[] teamIds = new int[selectedTeams.size()];
        for (int i = 0; i < selectedTeams.size(); i++) {
            teamIds[i] = selectedTeams.get(i).id;
        }
        settings.addIntArray(PlayersSelectorNodeModel.CFGKEY_TEAM_IDS, teamIds);

        // Also save first team ID for backwards compatibility with parent class
        int firstTeamId = teamIds.length > 0 ? teamIds[0] : -1;
        settings.addInt("teamId", firstTeamId);  // Parent class uses "teamId" key
    }
}
