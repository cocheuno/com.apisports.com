package com.apisports.knime.football.nodes.query.teams;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory for Teams query node.
 */
public class TeamsNodeFactory extends NodeFactory<TeamsNodeModel> {

    @Override
    public TeamsNodeModel createNodeModel() {
        return new TeamsNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<TeamsNodeModel> createNodeView(final int viewIndex,
                                                     final TeamsNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new TeamsNodeDialog();
    }
}
