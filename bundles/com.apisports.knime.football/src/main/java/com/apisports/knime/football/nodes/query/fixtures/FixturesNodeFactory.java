package com.apisports.knime.football.nodes.query.fixtures;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory for Fixtures query node.
 */
public class FixturesNodeFactory extends NodeFactory<FixturesNodeModel> {

    @Override
    public FixturesNodeModel createNodeModel() {
        return new FixturesNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<FixturesNodeModel> createNodeView(final int viewIndex,
                                                        final FixturesNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new FixturesNodeDialog();
    }
}
