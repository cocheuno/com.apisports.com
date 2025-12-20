package com.apisports.knime.football.nodes.query.standings;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory for Standings query node.
 */
public class StandingsNodeFactory extends NodeFactory<StandingsNodeModel> {

    @Override
    public StandingsNodeModel createNodeModel() {
        return new StandingsNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<StandingsNodeModel> createNodeView(final int viewIndex,
                                                         final StandingsNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new StandingsNodeDialog();
    }
}
