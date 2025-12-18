package com.apisports.knime.football.nodes.referencedata;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;

/**
 * Dialog for Reference Data Loader node.
 */
public class ReferenceDataLoaderNodeDialog extends DefaultNodeSettingsPane {

    protected ReferenceDataLoaderNodeDialog() {
        super();

        addDialogComponent(new DialogComponentBoolean(
            new SettingsModelBoolean(ReferenceDataLoaderNodeModel.CFGKEY_LOAD_TEAMS, true),
            "Load Teams (may take time for many leagues)"));

        addDialogComponent(new DialogComponentBoolean(
            new SettingsModelBoolean(ReferenceDataLoaderNodeModel.CFGKEY_LOAD_VENUES, false),
            "Load Venues (not yet implemented)"));

        addDialogComponent(new DialogComponentNumber(
            new SettingsModelInteger(ReferenceDataLoaderNodeModel.CFGKEY_CACHE_TTL, 86400),
            "Cache TTL (seconds):", 3600));
    }
}
