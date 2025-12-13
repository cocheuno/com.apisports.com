package com.apisports.knime.football.nodes.predictions;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class FootballPredictionsNodeFactory extends NodeFactory<FootballPredictionsNodeModel> {
    @Override
    public FootballPredictionsNodeModel createNodeModel() {
        return new FootballPredictionsNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<FootballPredictionsNodeModel> createNodeView(final int viewIndex,
            final FootballPredictionsNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new FootballPredictionsNodeDialog();
    }
}
