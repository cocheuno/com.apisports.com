package com.apisports.knime.football.nodes.query.odds;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory for Odds query node.
 */
public class OddsNodeFactory extends NodeFactory<OddsNodeModel> {

    @Override
    public OddsNodeModel createNodeModel() {
        return new OddsNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<OddsNodeModel> createNodeView(final int viewIndex,
                                                    final OddsNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new OddsNodeDialog();
    }
}
