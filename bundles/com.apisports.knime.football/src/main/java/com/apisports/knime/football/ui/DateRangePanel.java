package com.apisports.knime.football.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Reusable date range selection panel for API-Sports Football nodes.
 *
 * Provides 4 selection modes:
 * 1. Date Range - From/To with quick shortcuts
 * 2. Relative - Next/Last X matches/days/weeks
 * 3. Current Season - Auto from Reference Data
 * 4. Incremental - Since last run (flow variable)
 *
 * Serves 23 use cases requiring date filtering.
 */
public class DateRangePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    // Date mode constants
    public static final String MODE_RANGE = "range";
    public static final String MODE_RELATIVE = "relative";
    public static final String MODE_SEASON = "season";
    public static final String MODE_INCREMENTAL = "incremental";

    // Relative direction constants
    public static final String DIR_NEXT = "next";
    public static final String DIR_LAST = "last";

    // Relative unit constants
    public static final String UNIT_MATCHES = "matches";
    public static final String UNIT_DAYS = "days";
    public static final String UNIT_WEEKS = "weeks";
    public static final String UNIT_MONTHS = "months";

    // Settings models
    private final SettingsModelString m_dateMode;
    private final SettingsModelString m_fromDate;
    private final SettingsModelString m_toDate;
    private final SettingsModelString m_relativeDirection;
    private final SettingsModelInteger m_relativeCount;
    private final SettingsModelString m_relativeUnit;
    private final SettingsModelString m_incrementalVariable;

    // UI components
    private JRadioButton m_rangeModeRadio;
    private JRadioButton m_relativeModeRadio;
    private JRadioButton m_seasonModeRadio;
    private JRadioButton m_incrementalModeRadio;

    // Date range components
    private JTextField m_fromDateField;
    private JTextField m_toDateField;
    private JButton m_fromDateButton;
    private JButton m_toDateButton;
    private JPanel m_quickButtonsPanel;

    // Relative components
    private JComboBox<String> m_relativeDirectionCombo;
    private JSpinner m_relativeCountSpinner;
    private JComboBox<String> m_relativeUnitCombo;

    // Season components
    private JLabel m_seasonInfoLabel;

    // Incremental components
    private JTextField m_incrementalVariableField;
    private JLabel m_incrementalValueLabel;

    // Configuration
    private final String m_defaultIncrementalVariable;
    private final boolean m_allowIncremental;

    /**
     * Create date range panel.
     *
     * @param defaultIncrementalVariable Default flow variable name for incremental mode
     * @param allowIncremental Whether to show incremental mode option
     */
    public DateRangePanel(String defaultIncrementalVariable, boolean allowIncremental) {
        this.m_defaultIncrementalVariable = defaultIncrementalVariable;
        this.m_allowIncremental = allowIncremental;

        // Initialize settings models
        m_dateMode = new SettingsModelString("dateMode", MODE_RANGE);
        m_fromDate = new SettingsModelString("fromDate", LocalDate.now().toString());
        m_toDate = new SettingsModelString("toDate", LocalDate.now().plusDays(7).toString());
        m_relativeDirection = new SettingsModelString("relativeDirection", DIR_NEXT);
        m_relativeCount = new SettingsModelInteger("relativeCount", 10);
        m_relativeUnit = new SettingsModelString("relativeUnit", UNIT_MATCHES);
        m_incrementalVariable = new SettingsModelString("incrementalVariable", defaultIncrementalVariable);

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Date Selection"));

        // Main panel with all modes
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Radio button group
        ButtonGroup modeGroup = new ButtonGroup();
        m_rangeModeRadio = new JRadioButton("Date Range");
        m_relativeModeRadio = new JRadioButton("Relative (Next/Last X)");
        m_seasonModeRadio = new JRadioButton("Current Season");
        m_incrementalModeRadio = new JRadioButton("Incremental (Since Last Run)");

        modeGroup.add(m_rangeModeRadio);
        modeGroup.add(m_relativeModeRadio);
        modeGroup.add(m_seasonModeRadio);
        if (m_allowIncremental) {
            modeGroup.add(m_incrementalModeRadio);
        }

        // Default selection
        m_rangeModeRadio.setSelected(true);

        // Add action listeners to enable/disable panels
        ActionListener modeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updatePanelStates();
            }
        };
        m_rangeModeRadio.addActionListener(modeListener);
        m_relativeModeRadio.addActionListener(modeListener);
        m_seasonModeRadio.addActionListener(modeListener);
        if (m_allowIncremental) {
            m_incrementalModeRadio.addActionListener(modeListener);
        }

        // Create panels for each mode
        JPanel rangeModePanel = createRangeModePanel();
        JPanel relativeModePanel = createRelativeModePanel();
        JPanel seasonModePanel = createSeasonModePanel();
        JPanel incrementalModePanel = createIncrementalModePanel();

        // Add all components
        mainPanel.add(m_rangeModeRadio);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(rangeModePanel);
        mainPanel.add(Box.createVerticalStrut(10));

        mainPanel.add(m_relativeModeRadio);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(relativeModePanel);
        mainPanel.add(Box.createVerticalStrut(10));

        mainPanel.add(m_seasonModeRadio);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(seasonModePanel);

        if (m_allowIncremental) {
            mainPanel.add(Box.createVerticalStrut(10));
            mainPanel.add(m_incrementalModeRadio);
            mainPanel.add(Box.createVerticalStrut(5));
            mainPanel.add(incrementalModePanel);
        }

        add(mainPanel, BorderLayout.CENTER);

        // Initial state
        updatePanelStates();
    }

    private JPanel createRangeModePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

        // From date
        JPanel fromPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fromPanel.add(new JLabel("From:"));
        m_fromDateField = new JTextField(12);
        m_fromDateField.setText(LocalDate.now().toString());
        fromPanel.add(m_fromDateField);
        m_fromDateButton = new JButton("📅");
        m_fromDateButton.setToolTipText("Select date");
        fromPanel.add(m_fromDateButton);

        // To date
        JPanel toPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toPanel.add(new JLabel("To:  "));
        m_toDateField = new JTextField(12);
        m_toDateField.setText(LocalDate.now().plusDays(7).toString());
        toPanel.add(m_toDateField);
        m_toDateButton = new JButton("📅");
        m_toDateButton.setToolTipText("Select date");
        toPanel.add(m_toDateButton);

        // Quick buttons
        m_quickButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        m_quickButtonsPanel.add(new JLabel("Quick:"));
        addQuickButton("Today", 0, 0);
        addQuickButton("Tomorrow", 1, 1);
        addQuickButton("This Week", 0, 7);
        addQuickButton("Next 7", 0, 7);
        addQuickButton("Last 30", -30, 0);

        panel.add(fromPanel);
        panel.add(toPanel);
        panel.add(m_quickButtonsPanel);

        return panel;
    }

    private void addQuickButton(String label, int fromOffset, int toOffset) {
        JButton button = new JButton(label);
        button.addActionListener(e -> {
            LocalDate today = LocalDate.now();
            m_fromDateField.setText(today.plusDays(fromOffset).toString());
            m_toDateField.setText(today.plusDays(toOffset).toString());
        });
        m_quickButtonsPanel.add(button);
    }

    private JPanel createRelativeModePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

        m_relativeDirectionCombo = new JComboBox<>(new String[]{DIR_NEXT, DIR_LAST});
        m_relativeCountSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
        m_relativeUnitCombo = new JComboBox<>(new String[]{UNIT_MATCHES, UNIT_DAYS, UNIT_WEEKS, UNIT_MONTHS});

        panel.add(m_relativeDirectionCombo);
        panel.add(m_relativeCountSpinner);
        panel.add(m_relativeUnitCombo);

        return panel;
    }

    private JPanel createSeasonModePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

        m_seasonInfoLabel = new JLabel("Uses season start/end dates from Reference Data");
        m_seasonInfoLabel.setForeground(Color.GRAY);
        panel.add(m_seasonInfoLabel);

        return panel;
    }

    private JPanel createIncrementalModePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

        JPanel varPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        varPanel.add(new JLabel("Since flow variable:"));
        m_incrementalVariableField = new JTextField(15);
        m_incrementalVariableField.setText(m_defaultIncrementalVariable);
        varPanel.add(m_incrementalVariableField);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        m_incrementalValueLabel = new JLabel("Current value: (will be loaded at execution)");
        m_incrementalValueLabel.setForeground(Color.GRAY);
        infoPanel.add(m_incrementalValueLabel);

        JPanel helpPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel helpLabel = new JLabel("<html><i>Fetches only data newer than the flow variable date.<br>" +
                "Outputs updated flow variable with current timestamp.</i></html>");
        helpLabel.setForeground(Color.DARK_GRAY);
        helpPanel.add(helpLabel);

        panel.add(varPanel);
        panel.add(infoPanel);
        panel.add(helpPanel);

        return panel;
    }

    private void updatePanelStates() {
        // Enable/disable components based on selected mode
        boolean isRange = m_rangeModeRadio.isSelected();
        boolean isRelative = m_relativeModeRadio.isSelected();
        boolean isIncremental = m_incrementalModeRadio != null && m_incrementalModeRadio.isSelected();

        // Range mode components
        m_fromDateField.setEnabled(isRange);
        m_toDateField.setEnabled(isRange);
        m_fromDateButton.setEnabled(isRange);
        m_toDateButton.setEnabled(isRange);
        for (Component comp : m_quickButtonsPanel.getComponents()) {
            comp.setEnabled(isRange);
        }

        // Relative mode components
        m_relativeDirectionCombo.setEnabled(isRelative);
        m_relativeCountSpinner.setEnabled(isRelative);
        m_relativeUnitCombo.setEnabled(isRelative);

        // Incremental mode components
        if (m_incrementalVariableField != null) {
            m_incrementalVariableField.setEnabled(isIncremental);
        }
    }

    /**
     * Get the selected date mode.
     */
    public String getDateMode() {
        if (m_rangeModeRadio.isSelected()) return MODE_RANGE;
        if (m_relativeModeRadio.isSelected()) return MODE_RELATIVE;
        if (m_seasonModeRadio.isSelected()) return MODE_SEASON;
        if (m_incrementalModeRadio != null && m_incrementalModeRadio.isSelected()) return MODE_INCREMENTAL;
        return MODE_RANGE;
    }

    /**
     * Save settings to node settings.
     */
    public void saveSettingsTo(NodeSettingsWO settings) {
        // Save current mode
        settings.addString("dateMode", getDateMode());

        // Save all mode settings
        settings.addString("fromDate", m_fromDateField.getText());
        settings.addString("toDate", m_toDateField.getText());
        settings.addString("relativeDirection", (String) m_relativeDirectionCombo.getSelectedItem());
        settings.addInt("relativeCount", (Integer) m_relativeCountSpinner.getValue());
        settings.addString("relativeUnit", (String) m_relativeUnitCombo.getSelectedItem());
        if (m_incrementalVariableField != null) {
            settings.addString("incrementalVariable", m_incrementalVariableField.getText());
        }
    }

    /**
     * Load settings from node settings.
     */
    public void loadSettingsFrom(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {
        // Load mode
        String mode = settings.getString("dateMode", MODE_RANGE);

        // Select appropriate radio button
        switch (mode) {
            case MODE_RANGE:
                m_rangeModeRadio.setSelected(true);
                break;
            case MODE_RELATIVE:
                m_relativeModeRadio.setSelected(true);
                break;
            case MODE_SEASON:
                m_seasonModeRadio.setSelected(true);
                break;
            case MODE_INCREMENTAL:
                if (m_incrementalModeRadio != null) {
                    m_incrementalModeRadio.setSelected(true);
                }
                break;
        }

        // Load all settings (using default values, no exceptions thrown)
        m_fromDateField.setText(settings.getString("fromDate", LocalDate.now().toString()));
        m_toDateField.setText(settings.getString("toDate", LocalDate.now().plusDays(7).toString()));
        m_relativeDirectionCombo.setSelectedItem(settings.getString("relativeDirection", DIR_NEXT));
        m_relativeCountSpinner.setValue(settings.getInt("relativeCount", 10));
        m_relativeUnitCombo.setSelectedItem(settings.getString("relativeUnit", UNIT_MATCHES));
        if (m_incrementalVariableField != null) {
            m_incrementalVariableField.setText(
                settings.getString("incrementalVariable", m_defaultIncrementalVariable));
        }

        updatePanelStates();
    }

    /**
     * Get settings models for use in NodeModel.
     */
    public Map<String, Object> getSettingsModels() {
        Map<String, Object> models = new HashMap<>();
        models.put("dateMode", m_dateMode);
        models.put("fromDate", m_fromDate);
        models.put("toDate", m_toDate);
        models.put("relativeDirection", m_relativeDirection);
        models.put("relativeCount", m_relativeCount);
        models.put("relativeUnit", m_relativeUnit);
        models.put("incrementalVariable", m_incrementalVariable);
        return models;
    }
}
