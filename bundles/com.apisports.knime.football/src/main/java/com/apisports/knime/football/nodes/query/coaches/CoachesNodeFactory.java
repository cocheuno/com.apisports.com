package com.apisports.knime.football.nodes.query.coaches;

import org.knime.core.node.*;

public class CoachesNodeFactory extends NodeFactory<CoachesNodeModel> {
    @Override
    public CoachesNodeModel createNodeModel() {
        return new CoachesNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<CoachesNodeModel> createNodeView(final int viewIndex,
                                                          final CoachesNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new CoachesNodeDialog();
    }
}
