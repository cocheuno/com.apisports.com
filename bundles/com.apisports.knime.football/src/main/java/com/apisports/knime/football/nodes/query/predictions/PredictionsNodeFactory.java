package com.apisports.knime.football.nodes.query.predictions;

import org.knime.core.node.*;

public class PredictionsNodeFactory extends NodeFactory<PredictionsNodeModel> {
    @Override
    public PredictionsNodeModel createNodeModel() {
        return new PredictionsNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<PredictionsNodeModel> createNodeView(final int viewIndex,
                                                          final PredictionsNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new PredictionsNodeDialog();
    }
}
