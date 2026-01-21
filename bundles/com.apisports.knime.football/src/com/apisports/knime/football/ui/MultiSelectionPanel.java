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

package com.apisports.knime.football.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Reusable multi-selection panel for batch operations in API-Sports Football nodes.
 *
 * Features:
 * - Multi-select list with search/filter
 * - Select All / Clear All buttons
 * - Selection count display
 * - Pre-population from selector input port
 * - Generic type support for any selectable items
 *
 * Serves 12 use cases requiring batch operations (UC #21, #32, #35, #38, etc.)
 */
public class MultiSelectionPanel<T> extends JPanel {

    private static final long serialVersionUID = 1L;

    // UI components
    private JTextField m_searchField;
    private JList<SelectableItem<T>> m_itemList;
    private DefaultListModel<SelectableItem<T>> m_listModel;
    private DefaultListModel<SelectableItem<T>> m_filteredModel;
    private JLabel m_selectionCountLabel;
    private JButton m_selectAllButton;
    private JButton m_clearAllButton;
    private JLabel m_inputPortInfoLabel;

    // Configuration
    private final String m_panelTitle;
    private final Supplier<List<T>> m_itemLoader;
    private final ItemFormatter<T> m_formatter;
    private final String m_settingsKey;

    // State
    private List<T> m_allItems;
    private Set<T> m_preSelectedItems;
    private boolean m_hasInputPort;

    /**
     * Interface for formatting items in the list.
     */
    @FunctionalInterface
    public interface ItemFormatter<T> {
        String format(T item);
    }

    /**
     * Wrapper class to store both display text and original item.
     */
    private static class SelectableItem<T> {
        private final T item;
        private final String displayText;

        public SelectableItem(T item, String displayText) {
            this.item = item;
            this.displayText = displayText;
        }

        public T getItem() {
            return item;
        }

        public String getDisplayText() {
            return displayText;
        }

        @Override
        public String toString() {
            return displayText;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SelectableItem<?> other = (SelectableItem<?>) obj;
            return Objects.equals(item, other.item);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item);
        }
    }

    /**
     * Create multi-selection panel with string items.
     *
     * @param panelTitle Title for the panel border
     * @param itemLoader Callback to load available items
     * @param settingsKey Key for saving/loading selected items
     */
    public MultiSelectionPanel(String panelTitle, Supplier<List<String>> itemLoader, String settingsKey) {
        this(panelTitle, (Supplier<List<T>>) (Supplier<?>) itemLoader, item -> (String) item, settingsKey);
    }

    /**
     * Create multi-selection panel with custom item type.
     *
     * @param panelTitle Title for the panel border
     * @param itemLoader Callback to load available items
     * @param formatter Formatter to convert items to display strings
     * @param settingsKey Key for saving/loading selected items
     */
    public MultiSelectionPanel(String panelTitle, Supplier<List<T>> itemLoader,
                                ItemFormatter<T> formatter, String settingsKey) {
        this.m_panelTitle = panelTitle;
        this.m_itemLoader = itemLoader;
        this.m_formatter = formatter;
        this.m_settingsKey = settingsKey;
        this.m_allItems = new ArrayList<>();
        this.m_preSelectedItems = new HashSet<>();
        this.m_hasInputPort = false;

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder(m_panelTitle));

        // Top panel: Search and buttons
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        m_searchField = new JTextField(20);
        m_searchField.setToolTipText("Filter items by text");
        searchPanel.add(m_searchField);
        JLabel searchIcon = new JLabel("🔍");
        searchPanel.add(searchIcon);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        m_selectAllButton = new JButton("Select All");
        m_clearAllButton = new JButton("Clear All");
        buttonsPanel.add(m_selectAllButton);
        buttonsPanel.add(m_clearAllButton);

        topPanel.add(searchPanel, BorderLayout.WEST);
        topPanel.add(buttonsPanel, BorderLayout.EAST);

        // Center panel: List
        m_listModel = new DefaultListModel<>();
        m_filteredModel = new DefaultListModel<>();
        m_itemList = new JList<>(m_filteredModel);
        m_itemList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_itemList.setVisibleRowCount(10);

        JScrollPane scrollPane = new JScrollPane(m_itemList);
        scrollPane.setPreferredSize(new Dimension(400, 200));

        // Bottom panel: Selection count and info
        JPanel bottomPanel = new JPanel(new BorderLayout());

        m_selectionCountLabel = new JLabel("Selected: 0 items");
        m_selectionCountLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        m_inputPortInfoLabel = new JLabel("");
        m_inputPortInfoLabel.setForeground(new Color(255, 140, 0)); // Orange
        m_inputPortInfoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        m_inputPortInfoLabel.setVisible(false);

        bottomPanel.add(m_selectionCountLabel, BorderLayout.WEST);
        bottomPanel.add(m_inputPortInfoLabel, BorderLayout.SOUTH);

        // Add all panels
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Add listeners
        addListeners();
    }

    private void addListeners() {
        // Search field listener - filter items as user types
        m_searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterItems();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterItems();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterItems();
            }
        });

        // List selection listener - update count
        m_itemList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectionCount();
            }
        });

        // Select All button
        m_selectAllButton.addActionListener(e -> {
            m_itemList.setSelectionInterval(0, m_filteredModel.getSize() - 1);
        });

        // Clear All button
        m_clearAllButton.addActionListener(e -> {
            m_itemList.clearSelection();
        });
    }

    private void filterItems() {
        String searchText = m_searchField.getText().toLowerCase().trim();

        // Save current selection
        List<SelectableItem<T>> currentSelection = m_itemList.getSelectedValuesList();

        // Clear and rebuild filtered model
        m_filteredModel.clear();

        if (searchText.isEmpty()) {
            // No filter - show all items
            for (int i = 0; i < m_listModel.getSize(); i++) {
                m_filteredModel.addElement(m_listModel.getElementAt(i));
            }
        } else {
            // Filter items by search text
            for (int i = 0; i < m_listModel.getSize(); i++) {
                SelectableItem<T> item = m_listModel.getElementAt(i);
                if (item.getDisplayText().toLowerCase().contains(searchText)) {
                    m_filteredModel.addElement(item);
                }
            }
        }

        // Restore selection
        List<Integer> indicesToSelect = new ArrayList<>();
        for (int i = 0; i < m_filteredModel.getSize(); i++) {
            if (currentSelection.contains(m_filteredModel.getElementAt(i))) {
                indicesToSelect.add(i);
            }
        }

        if (!indicesToSelect.isEmpty()) {
            int[] indices = indicesToSelect.stream().mapToInt(Integer::intValue).toArray();
            m_itemList.setSelectedIndices(indices);
        }

        updateSelectionCount();
    }

    private void updateSelectionCount() {
        int selectedCount = m_itemList.getSelectedIndices().length;
        int totalCount = m_filteredModel.getSize();
        m_selectionCountLabel.setText("Selected: " + selectedCount + " of " + totalCount + " items");
    }

    /**
     * Load available items into the list.
     */
    public void loadItems() {
        try {
            m_allItems = m_itemLoader.get();
            populateList();
        } catch (Exception e) {
            // Handle gracefully - show empty list
            m_allItems = new ArrayList<>();
            populateList();
        }
    }

    private void populateList() {
        m_listModel.clear();
        m_filteredModel.clear();

        for (T item : m_allItems) {
            String displayText = m_formatter.format(item);
            SelectableItem<T> selectableItem = new SelectableItem<>(item, displayText);
            m_listModel.addElement(selectableItem);
            m_filteredModel.addElement(selectableItem);
        }

        // Pre-select items if available
        if (!m_preSelectedItems.isEmpty()) {
            List<Integer> indicesToSelect = new ArrayList<>();
            for (int i = 0; i < m_filteredModel.getSize(); i++) {
                SelectableItem<T> item = m_filteredModel.getElementAt(i);
                if (m_preSelectedItems.contains(item.getItem())) {
                    indicesToSelect.add(i);
                }
            }

            if (!indicesToSelect.isEmpty()) {
                int[] indices = indicesToSelect.stream().mapToInt(Integer::intValue).toArray();
                m_itemList.setSelectedIndices(indices);
            }
        }

        updateSelectionCount();
    }

    /**
     * Set items to be pre-selected (e.g., from selector input port).
     */
    public void setPreSelectedItems(Collection<T> items) {
        m_preSelectedItems = new HashSet<>(items);
        m_hasInputPort = !items.isEmpty();

        if (m_hasInputPort) {
            m_inputPortInfoLabel.setText(
                "⚠ Input port connected: " + items.size() + " items pre-selected. You can modify selection below.");
            m_inputPortInfoLabel.setVisible(true);
        } else {
            m_inputPortInfoLabel.setVisible(false);
        }

        // If list is already populated, update selection
        if (!m_allItems.isEmpty()) {
            populateList();
        }
    }

    /**
     * Get currently selected items.
     */
    public List<T> getSelectedItems() {
        return m_itemList.getSelectedValuesList().stream()
            .map(SelectableItem::getItem)
            .collect(Collectors.toList());
    }

    /**
     * Set selected items programmatically.
     */
    public void setSelectedItems(Collection<T> items) {
        m_preSelectedItems = new HashSet<>(items);
        populateList();
    }

    /**
     * Get all available items.
     */
    public List<T> getAllItems() {
        return new ArrayList<>(m_allItems);
    }

    /**
     * Check if input port is connected (has pre-selected items).
     */
    public boolean hasInputPort() {
        return m_hasInputPort;
    }

    /**
     * Save selected items to node settings.
     * For string items, saves directly. For complex types, must be converted by caller.
     */
    public void saveSettingsTo(NodeSettingsWO settings) {
        List<T> selectedItems = getSelectedItems();

        // Convert items to strings for storage
        String[] itemStrings = selectedItems.stream()
            .map(m_formatter::format)
            .toArray(String[]::new);

        settings.addStringArray(m_settingsKey, itemStrings);
        settings.addBoolean(m_settingsKey + "_hasInputPort", m_hasInputPort);
    }

    /**
     * Load selected items from node settings.
     */
    public void loadSettingsFrom(NodeSettingsRO settings, PortObjectSpec[] specs)
            throws NotConfigurableException {
        // Load previously selected items (using default values, no exceptions thrown)
        String[] savedItems = settings.getStringArray(m_settingsKey, new String[]{});
        boolean hadInputPort = settings.getBoolean(m_settingsKey + "_hasInputPort", false);

        // Load all available items
        loadItems();

        // Find matching items and pre-select them
        Set<T> itemsToSelect = new HashSet<>();
        for (String savedItem : savedItems) {
            for (T item : m_allItems) {
                if (m_formatter.format(item).equals(savedItem)) {
                    itemsToSelect.add(item);
                    break;
                }
            }
        }

        setPreSelectedItems(itemsToSelect);

        // If there was an input port before but not in these specs, clear the flag
        if (hadInputPort && !hasInputPortInSpecs(specs)) {
            m_hasInputPort = false;
            m_inputPortInfoLabel.setVisible(false);
        }
    }

    private boolean hasInputPortInSpecs(PortObjectSpec[] specs) {
        // Check if optional input port exists and is connected
        // Subclasses can override this logic based on their port configuration
        if (specs == null || specs.length < 3) {
            return false;
        }
        return specs[2] != null;
    }

    /**
     * Get the settings key used by this panel.
     */
    public String getSettingsKey() {
        return m_settingsKey;
    }

    /**
     * Clear all selections.
     */
    public void clearSelection() {
        m_itemList.clearSelection();
    }

    /**
     * Select all items.
     */
    public void selectAll() {
        m_itemList.setSelectionInterval(0, m_filteredModel.getSize() - 1);
    }

    /**
     * Get selection count.
     */
    public int getSelectionCount() {
        return m_itemList.getSelectedIndices().length;
    }

    /**
     * Enable or disable the panel.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        m_searchField.setEnabled(enabled);
        m_itemList.setEnabled(enabled);
        m_selectAllButton.setEnabled(enabled);
        m_clearAllButton.setEnabled(enabled);
    }
}
