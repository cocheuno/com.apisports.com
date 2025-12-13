package com.apisports.knime.football.nodes.fixtures;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class FootballFixturesNodeFactory extends NodeFactory<FootballFixturesNodeModel> {
    @Override
    public FootballFixturesNodeModel createNodeModel() {
        return new FootballFixturesNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<FootballFixturesNodeModel> createNodeView(final int viewIndex,
            final FootballFixturesNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new FootballFixturesNodeDialog();
    }
}
