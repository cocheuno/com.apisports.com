package com.apisports.knime.football.nodes.query.venues;

import org.knime.core.node.*;

public class VenuesNodeFactory extends NodeFactory<VenuesNodeModel> {
    @Override
    public VenuesNodeModel createNodeModel() {
        return new VenuesNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<VenuesNodeModel> createNodeView(final int viewIndex, final VenuesNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new VenuesNodeDialog();
    }
}
