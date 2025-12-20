package com.apisports.knime.football.nodes.query.players;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory for Players query node.
 */
public class PlayersNodeFactory extends NodeFactory<PlayersNodeModel> {

    @Override
    public PlayersNodeModel createNodeModel() {
        return new PlayersNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<PlayersNodeModel> createNodeView(final int viewIndex,
                                                       final PlayersNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new PlayersNodeDialog();
    }
}
