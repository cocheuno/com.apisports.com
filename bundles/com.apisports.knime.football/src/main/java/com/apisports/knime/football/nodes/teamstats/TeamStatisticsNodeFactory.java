package com.apisports.knime.football.nodes.teamstats;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Node factory for Team Statistics query node.
 * Uses reference data to provide dropdown-based query building.
 */
public class TeamStatisticsNodeFactory extends NodeFactory<TeamStatisticsNodeModel> {

    @Override
    public TeamStatisticsNodeModel createNodeModel() {
        return new TeamStatisticsNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<TeamStatisticsNodeModel> createNodeView(final int viewIndex,
                                                             final TeamStatisticsNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new TeamStatisticsNodeDialog();
    }
}
