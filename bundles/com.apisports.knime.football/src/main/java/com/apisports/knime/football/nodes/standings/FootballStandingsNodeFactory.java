package com.apisports.knime.football.nodes.standings;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class FootballStandingsNodeFactory extends NodeFactory<FootballStandingsNodeModel> {
    @Override
    public FootballStandingsNodeModel createNodeModel() {
        return new FootballStandingsNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<FootballStandingsNodeModel> createNodeView(final int viewIndex,
            final FootballStandingsNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new FootballStandingsNodeDialog();
    }
}
