package com.apisports.knime.football.nodes.query.predictions;

import com.apisports.knime.football.nodes.query.AbstractFootballQueryNodeDialog;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.*;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PredictionsNodeDialog extends AbstractFootballQueryNodeDialog {

    private JComboBox<String> fixtureCombo;
    private List<FixtureItem> availableFixtures;

    /**
     * Helper class to store fixture information for the dropdown.
     */
    private static class FixtureItem {
        final String fixtureId;
        final String homeTeam;
        final String awayTeam;

        FixtureItem(String fixtureId, String homeTeam, String awayTeam) {
            this.fixtureId = fixtureId;
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
        }

        @Override
        public String toString() {
            return fixtureId + ": " + homeTeam + " vs " + awayTeam;
        }
    }

    public PredictionsNodeDialog() {
        super();
        availableFixtures = new ArrayList<>();
        addPredictionsSpecificComponents();
    }

    private void addPredictionsSpecificComponents() {
        // Add separator
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        mainPanel.add(Box.createVerticalStrut(10));

        // Fixture selection dropdown (editable for manual input)
        JPanel fixturePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fixturePanel.add(new JLabel("Fixture ID:"));
        fixtureCombo = new JComboBox<>();
        fixtureCombo.setEditable(true); // Allow users to type fixture ID directly
        fixtureCombo.setPreferredSize(new Dimension(400, 25));
        fixturePanel.add(fixtureCombo);
        mainPanel.add(fixturePanel);

        // Add help text
        JTextArea helpText = new JTextArea(
            "Workflow:\n" +
            "1. Configure and execute a Fixtures node for your desired League/Season/Team\n" +
            "2. Look at the Fixture_ID column in the Fixtures output to find fixtures\n" +
            "3. Enter or select a Fixture ID above\n" +
            "4. Execute this node to get predictions for that fixture\n\n" +
            "Tip: You can type the Fixture ID directly or connect the Fixtures node output\n" +
            "to this node's optional third input port (coming soon: auto-populate from connected port).\n\n" +
            "Note: Only upcoming or recent fixtures will have predictions available."
        );
        helpText.setEditable(false);
        helpText.setWrapStyleWord(true);
        helpText.setLineWrap(true);
        helpText.setBackground(mainPanel.getBackground());
        helpText.setFont(new Font("SansSerif", Font.ITALIC, 10));
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(helpText);
    }

    @Override
    protected void loadAdditionalSettings(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {

        // Check if optional fixtures port (port 2) is connected
        if (specs.length > 2 && specs[2] != null && specs[2] instanceof DataTableSpec) {
            DataTableSpec fixturesSpec = (DataTableSpec) specs[2];
            populateFixturesFromPort(fixturesSpec);
        } else {
            // No fixtures port connected - clear combo
            fixtureCombo.removeAllItems();
        }

        // Restore saved fixture ID (set in editor for editable combo)
        String savedFixtureId = settings.getString(PredictionsNodeModel.CFGKEY_FIXTURE_ID, "");
        if (!savedFixtureId.isEmpty()) {
            // First try to select from dropdown if it matches
            boolean found = selectFixture(savedFixtureId);
            // If not found in dropdown, set directly in editor
            if (!found) {
                fixtureCombo.getEditor().setItem(savedFixtureId);
            }
        }
    }

    /**
     * Populate fixture combo from the optional fixtures input port.
     * Filters by selected team if team is chosen.
     */
    private void populateFixturesFromPort(DataTableSpec fixturesSpec) {
        fixtureCombo.removeAllItems();
        availableFixtures.clear();

        // Find columns in the fixtures table
        int fixtureIdIdx = fixturesSpec.findColumnIndex("Fixture_ID");
        int homeTeamIdx = fixturesSpec.findColumnIndex("Home_Team");
        int awayTeamIdx = fixturesSpec.findColumnIndex("Away_Team");

        if (fixtureIdIdx < 0 || homeTeamIdx < 0 || awayTeamIdx < 0) {
            fixtureCombo.addItem("-- Fixtures table missing required columns --");
            return;
        }

        // Note: In KNIME dialogs, we only have access to PortObjectSpec, not the actual data
        // The spec tells us the structure but not the rows. To access actual fixture rows,
        // we would need the PortObject which is only available during execute(), not configure().

        // Add a helpful message
        fixtureCombo.addItem("-- Select and execute Fixtures node first --");
    }

    /**
     * Select fixture by ID in the combo box.
     * @return true if fixture was found and selected, false otherwise
     */
    private boolean selectFixture(String fixtureId) {
        for (int i = 0; i < fixtureCombo.getItemCount(); i++) {
            String item = fixtureCombo.getItemAt(i);
            if (item != null && item.startsWith(fixtureId + ":")) {
                fixtureCombo.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void saveAdditionalSettings(NodeSettingsWO settings) throws InvalidSettingsException {
        // Get fixture ID from combo (editable, so might be typed text or selected item)
        Object selected = fixtureCombo.getEditor().getItem();
        String fixtureId = "";

        if (selected != null) {
            String selectedStr = selected.toString().trim();
            // If format is "123: Team A vs Team B", extract just the ID
            if (selectedStr.contains(":")) {
                fixtureId = selectedStr.substring(0, selectedStr.indexOf(":")).trim();
            } else {
                // User typed ID directly
                fixtureId = selectedStr;
            }
        }

        settings.addString(PredictionsNodeModel.CFGKEY_FIXTURE_ID, fixtureId);
    }
}
