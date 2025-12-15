package com.apisports.knime.football.nodes.teams;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * NodeFactory for the Football Teams node.
 */
public class FootballTeamsNodeFactory extends NodeFactory<FootballTeamsNodeModel> {

    @Override
    public FootballTeamsNodeModel createNodeModel() {
        return new FootballTeamsNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<FootballTeamsNodeModel> createNodeView(final int viewIndex,
                                                            final FootballTeamsNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new FootballTeamsNodeDialog();
    }
}
