package com.apisports.knime.football.nodes.query.trophies;

import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * Node dialog for Trophies node.
 *
 * This node has no configuration options - it automatically processes
 * player or coach IDs from the connected input node.
 */
public class TrophiesNodeDialog extends DefaultNodeSettingsPane {
    public TrophiesNodeDialog() {
        super();
        // No configuration needed - node auto-configures based on input
        addDialogComponent(new org.knime.core.node.defaultnodesettings.DialogComponentLabel(
            "This node automatically queries trophies for all players or coaches " +
            "from the connected input node.\n\n" +
            "Connect a Players or Coaches node to the third input port.\n\n" +
            "The node will detect whether the input is from Players or Coaches " +
            "and query the appropriate trophies."
        ));
    }
}
