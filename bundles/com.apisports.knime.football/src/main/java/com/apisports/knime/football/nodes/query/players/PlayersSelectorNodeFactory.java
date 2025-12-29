package com.apisports.knime.football.nodes.query.players;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory for Players Selector node.
 */
public class PlayersSelectorNodeFactory extends NodeFactory<PlayersSelectorNodeModel> {

    @Override
    public PlayersSelectorNodeModel createNodeModel() {
        return new PlayersSelectorNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<PlayersSelectorNodeModel> createNodeView(final int viewIndex,
                                                        final PlayersSelectorNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new PlayersSelectorNodeDialog();
    }
}
