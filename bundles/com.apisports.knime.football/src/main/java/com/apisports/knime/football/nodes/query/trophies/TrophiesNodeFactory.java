package com.apisports.knime.football.nodes.query.trophies;

import org.knime.core.node.*;

public class TrophiesNodeFactory extends NodeFactory<TrophiesNodeModel> {
    @Override
    public TrophiesNodeModel createNodeModel() {
        return new TrophiesNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<TrophiesNodeModel> createNodeView(final int viewIndex,
                                                          final TrophiesNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new TrophiesNodeDialog();
    }
}
