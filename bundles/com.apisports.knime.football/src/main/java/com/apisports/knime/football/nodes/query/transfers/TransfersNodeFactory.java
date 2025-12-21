package com.apisports.knime.football.nodes.query.transfers;

import org.knime.core.node.*;

public class TransfersNodeFactory extends NodeFactory<TransfersNodeModel> {
    @Override
    public TransfersNodeModel createNodeModel() {
        return new TransfersNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<TransfersNodeModel> createNodeView(final int viewIndex,
                                                          final TransfersNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new TransfersNodeDialog();
    }
}
