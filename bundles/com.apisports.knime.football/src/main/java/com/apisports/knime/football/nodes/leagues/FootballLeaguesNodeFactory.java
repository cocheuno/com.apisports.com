package com.apisports.knime.football.nodes.leagues;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * NodeFactory for the Football Leagues node.
 */
public class FootballLeaguesNodeFactory extends NodeFactory<FootballLeaguesNodeModel> {

    @Override
    public FootballLeaguesNodeModel createNodeModel() {
        return new FootballLeaguesNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<FootballLeaguesNodeModel> createNodeView(final int viewIndex,
            final FootballLeaguesNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new FootballLeaguesNodeDialog();
    }
}
