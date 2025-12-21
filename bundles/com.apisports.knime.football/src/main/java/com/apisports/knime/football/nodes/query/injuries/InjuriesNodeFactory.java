package com.apisports.knime.football.nodes.query.injuries;

import org.knime.core.node.*;

public class InjuriesNodeFactory extends NodeFactory<InjuriesNodeModel> {
    @Override
    public InjuriesNodeModel createNodeModel() {
        return new InjuriesNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<InjuriesNodeModel> createNodeView(final int viewIndex, final InjuriesNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new InjuriesNodeDialog();
    }
}
