package com.apisports.knime.connector.nodes.stats;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * NodeFactory for the API Statistics node.
 */
public class ApiStatisticsNodeFactory extends NodeFactory<ApiStatisticsNodeModel> {

    @Override
    public ApiStatisticsNodeModel createNodeModel() {
        return new ApiStatisticsNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<ApiStatisticsNodeModel> createNodeView(final int viewIndex,
            final ApiStatisticsNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return false;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return null;
    }
}
