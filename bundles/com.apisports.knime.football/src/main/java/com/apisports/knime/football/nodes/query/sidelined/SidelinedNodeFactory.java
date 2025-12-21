package com.apisports.knime.football.nodes.query.sidelined;

import org.knime.core.node.*;

public class SidelinedNodeFactory extends NodeFactory<SidelinedNodeModel> {
    @Override
    public SidelinedNodeModel createNodeModel() {
        return new SidelinedNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<SidelinedNodeModel> createNodeView(final int viewIndex,
                                                          final SidelinedNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new SidelinedNodeDialog();
    }
}
