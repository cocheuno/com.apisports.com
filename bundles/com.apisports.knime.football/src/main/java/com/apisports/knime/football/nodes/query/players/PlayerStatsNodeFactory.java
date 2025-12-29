package com.apisports.knime.football.nodes.query.players;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory for Player Stats node.
 */
public class PlayerStatsNodeFactory extends NodeFactory<PlayerStatsNodeModel> {

    @Override
    public PlayerStatsNodeModel createNodeModel() {
        return new PlayerStatsNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<PlayerStatsNodeModel> createNodeView(final int viewIndex,
                                                        final PlayerStatsNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new PlayerStatsNodeDialog();
    }
}
