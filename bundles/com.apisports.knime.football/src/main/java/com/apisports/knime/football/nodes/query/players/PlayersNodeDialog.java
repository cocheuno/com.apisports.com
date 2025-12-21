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

/**
 * Dialog for Players query node.
 */
public class PlayersNodeDialog extends AbstractFootballQueryNodeDialog {

    private JComboBox<String> queryTypeCombo;
    private JTextField playerNameField;
    private JTextField playerIdField;
    private JPanel nameSearchPanel;
    private JPanel idSearchPanel;

    public PlayersNodeDialog() {
        super();
        addPlayersSpecificComponents();
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
            "• Top Scorers/Assists/Cards: Get league leaders (optionally filter by team)\n" +
            "• Players by Team: Get all players for a specific team\n" +
            "• Search by Name: Find players by name (partial match)\n" +
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

    private void updateVisibilityForQueryType() {
        String queryType = (String) queryTypeCombo.getSelectedItem();

        boolean showNameSearch = PlayersNodeModel.QUERY_BY_NAME.equals(queryType);
        boolean showIdSearch = PlayersNodeModel.QUERY_BY_ID.equals(queryType);
        boolean showLeagueSeason = !PlayersNodeModel.QUERY_BY_ID.equals(queryType)
                                    && !PlayersNodeModel.QUERY_BY_TEAM.equals(queryType);
        boolean enableTeamCombo = !PlayersNodeModel.QUERY_BY_ID.equals(queryType);

        nameSearchPanel.setVisible(showNameSearch);
        idSearchPanel.setVisible(showIdSearch);
        leagueCombo.setEnabled(showLeagueSeason);
        seasonCombo.setEnabled(true); // Always needed
        teamCombo.setEnabled(enableTeamCombo);

        // Hide the confusing checkbox for Players node
        teamOptionalCheckbox.setVisible(false);

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

        queryTypeCombo.setSelectedItem(queryType);
        playerNameField.setText(playerName);
        playerIdField.setText(playerId);

        updateVisibilityForQueryType();
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        settings.addString(PlayersNodeModel.CFGKEY_QUERY_TYPE,
                          (String) queryTypeCombo.getSelectedItem());
        settings.addString(PlayersNodeModel.CFGKEY_PLAYER_NAME, playerNameField.getText());
        settings.addString(PlayersNodeModel.CFGKEY_PLAYER_ID, playerIdField.getText());
    }
}
