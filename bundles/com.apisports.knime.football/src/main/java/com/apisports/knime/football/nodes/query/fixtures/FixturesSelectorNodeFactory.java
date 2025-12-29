package com.apisports.knime.football.nodes.query.fixtures;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Factory for Fixtures Selector node.
 */
public class FixturesSelectorNodeFactory extends NodeFactory<FixturesSelectorNodeModel> {

    @Override
    public FixturesSelectorNodeModel createNodeModel() {
        return new FixturesSelectorNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<FixturesSelectorNodeModel> createNodeView(final int viewIndex,
                                                        final FixturesSelectorNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new FixturesSelectorNodeDialog();
    }
}
