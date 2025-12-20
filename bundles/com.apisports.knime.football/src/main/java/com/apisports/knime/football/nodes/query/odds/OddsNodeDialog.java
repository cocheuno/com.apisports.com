package com.apisports.knime.football.nodes.query.odds;

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
 * Dialog for Odds query node.
 */
public class OddsNodeDialog extends AbstractFootballQueryNodeDialog {

    private JComboBox<String> queryTypeCombo;
    private JTextField fixtureIdField;
    private JTextField bookmakerField;
    private JTextField betTypeField;
    private JPanel fixtureIdPanel;

    public OddsNodeDialog() {
        super();
        addOddsSpecificComponents();
    }

    private void addOddsSpecificComponents() {
        // Add separator
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        mainPanel.add(Box.createVerticalStrut(10));

        // Query type selection
        JPanel queryTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        queryTypePanel.add(new JLabel("Query Type:"));
        queryTypeCombo = new JComboBox<>(new String[]{
            OddsNodeModel.QUERY_BY_FIXTURE,
            OddsNodeModel.QUERY_BY_LEAGUE,
            OddsNodeModel.QUERY_LIVE
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

        // Fixture ID panel
        fixtureIdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fixtureIdPanel.add(new JLabel("Fixture ID:"));
        fixtureIdField = new JTextField(15);
        fixtureIdPanel.add(fixtureIdField);
        mainPanel.add(fixtureIdPanel);

        // Optional filters
        JPanel bookmakerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bookmakerPanel.add(new JLabel("Bookmaker (optional):"));
        bookmakerField = new JTextField(20);
        bookmakerPanel.add(bookmakerField);
        mainPanel.add(bookmakerPanel);

        JPanel betTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        betTypePanel.add(new JLabel("Bet Type (optional):"));
        betTypeField = new JTextField(20);
        betTypePanel.add(betTypeField);
        mainPanel.add(betTypePanel);

        // Add help text
        JTextArea helpText = new JTextArea(
            "Query Types:\n" +
            "• By Fixture ID: Get odds for a specific fixture\n" +
            "• By League/Season: Get odds for all fixtures in league/season\n" +
            "• Live Odds: Get odds for currently live fixtures\n\n" +
            "Common Bookmakers: Bet365, Betway, William Hill, etc.\n" +
            "Common Bet Types: Match Winner, Goals Over/Under, Both Teams Score, etc."
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

        boolean showFixtureId = OddsNodeModel.QUERY_BY_FIXTURE.equals(queryType);
        boolean showLeague = OddsNodeModel.QUERY_BY_LEAGUE.equals(queryType);
        boolean showLive = OddsNodeModel.QUERY_LIVE.equals(queryType);

        fixtureIdPanel.setVisible(showFixtureId);
        leagueCombo.setEnabled(showLeague);
        seasonCombo.setEnabled(showLeague);

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    @Override
    protected void loadAdditionalSettings(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {
        String queryType = settings.getString(OddsNodeModel.CFGKEY_QUERY_TYPE,
                                              OddsNodeModel.QUERY_BY_FIXTURE);
        String fixtureId = settings.getString(OddsNodeModel.CFGKEY_FIXTURE_ID, "");
        String bookmaker = settings.getString(OddsNodeModel.CFGKEY_BOOKMAKER, "");
        String betType = settings.getString(OddsNodeModel.CFGKEY_BET_TYPE, "");

        queryTypeCombo.setSelectedItem(queryType);
        fixtureIdField.setText(fixtureId);
        bookmakerField.setText(bookmaker);
        betTypeField.setText(betType);

        updateVisibilityForQueryType();
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        settings.addString(OddsNodeModel.CFGKEY_QUERY_TYPE,
                          (String) queryTypeCombo.getSelectedItem());
        settings.addString(OddsNodeModel.CFGKEY_FIXTURE_ID, fixtureIdField.getText());
        settings.addString(OddsNodeModel.CFGKEY_BOOKMAKER, bookmakerField.getText());
        settings.addString(OddsNodeModel.CFGKEY_BET_TYPE, betTypeField.getText());
    }
}
