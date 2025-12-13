package com.apisports.knime.connector.nodes;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * NodeFactory for the API-Sports Connector node.
 */
public class ApiSportsConnectorNodeFactory extends NodeFactory<ApiSportsConnectorNodeModel> {

    @Override
    public ApiSportsConnectorNodeModel createNodeModel() {
        return new ApiSportsConnectorNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<ApiSportsConnectorNodeModel> createNodeView(final int viewIndex,
            final ApiSportsConnectorNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new ApiSportsConnectorNodeDialog();
    }
}
