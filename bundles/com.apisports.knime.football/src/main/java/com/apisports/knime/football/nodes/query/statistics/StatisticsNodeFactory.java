package com.apisports.knime.football.nodes.query.statistics;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class StatisticsNodeFactory extends NodeFactory<StatisticsNodeModel> {

    @Override
    public StatisticsNodeModel createNodeModel() {
        return new StatisticsNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<StatisticsNodeModel> createNodeView(final int viewIndex,
                                                          final StatisticsNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new StatisticsNodeDialog();
    }
}
