package com.apisports.knime.football.nodes.referencedata;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import java.util.Arrays;
import java.util.List;

/**
 * Dialog for Reference Data Loader node.
 */
public class ReferenceDataLoaderNodeDialog extends DefaultNodeSettingsPane {

    protected ReferenceDataLoaderNodeDialog() {
        super();

        createNewGroup("Database Configuration");

        addDialogComponent(new DialogComponentFileChooser(
            new SettingsModelString(ReferenceDataLoaderNodeModel.CFGKEY_DB_PATH,
                ReferenceDataLoaderNodeModel.getDefaultDbPath()),
            "history-db-path",
            0,
            ".db"));

        addDialogComponent(new DialogComponentBoolean(
            new SettingsModelBoolean(ReferenceDataLoaderNodeModel.CFGKEY_CLEAR_AND_RELOAD, false),
            "Clear & Reload (wipe existing data before loading)"));

        closeCurrentGroup();

        createNewGroup("Data Filters");

        // Country filter - major football countries
        List<String> availableCountries = Arrays.asList(
            "Argentina", "Australia", "Austria", "Belgium", "Brazil", "Bulgaria",
            "Chile", "China", "Colombia", "Croatia", "Czech-Republic", "Denmark",
            "England", "Finland", "France", "Germany", "Greece", "Hungary",
            "India", "Italy", "Japan", "Mexico", "Netherlands", "Norway",
            "Poland", "Portugal", "Romania", "Russia", "Scotland", "Serbia",
            "Slovakia", "South-Korea", "Spain", "Sweden", "Switzerland", "Turkey",
            "Ukraine", "Uruguay", "USA", "Wales"
        );

        addDialogComponent(new DialogComponentStringListSelection(
            new SettingsModelStringArray(ReferenceDataLoaderNodeModel.CFGKEY_COUNTRY_FILTER, new String[0]),
            "Filter by Countries (leave empty for all):",
            availableCountries,
            false,
            10));

        closeCurrentGroup();

        createNewGroup("Data Loading Options");

        addDialogComponent(new DialogComponentBoolean(
            new SettingsModelBoolean(ReferenceDataLoaderNodeModel.CFGKEY_LOAD_TEAMS, true),
            "Load Teams (may take time for many leagues)"));

        addDialogComponent(new DialogComponentBoolean(
            new SettingsModelBoolean(ReferenceDataLoaderNodeModel.CFGKEY_LOAD_VENUES, false),
            "Load Venues (not yet implemented)"));

        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(ReferenceDataLoaderNodeModel.CFGKEY_CACHE_TTL, 86400),
            "Cache TTL (seconds):", 3600));

        closeCurrentGroup();
    }
}
