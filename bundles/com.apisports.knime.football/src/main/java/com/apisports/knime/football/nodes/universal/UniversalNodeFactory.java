package com.apisports.knime.football.nodes.universal;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * NodeFactory for the Universal API Request node.
 */
public class UniversalNodeFactory extends NodeFactory<UniversalNodeModel> {

    @Override
    public UniversalNodeModel createNodeModel() {
        return new UniversalNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<UniversalNodeModel> createNodeView(final int viewIndex,
                                                         final UniversalNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new UniversalNodeDialog();
    }
}
