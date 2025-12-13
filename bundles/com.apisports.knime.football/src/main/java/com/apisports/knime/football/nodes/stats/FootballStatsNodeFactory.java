package com.apisports.knime.football.nodes.stats;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class FootballStatsNodeFactory extends NodeFactory<FootballStatsNodeModel> {
    @Override
    public FootballStatsNodeModel createNodeModel() {
        return new FootballStatsNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<FootballStatsNodeModel> createNodeView(final int viewIndex,
            final FootballStatsNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new FootballStatsNodeDialog();
    }
}
