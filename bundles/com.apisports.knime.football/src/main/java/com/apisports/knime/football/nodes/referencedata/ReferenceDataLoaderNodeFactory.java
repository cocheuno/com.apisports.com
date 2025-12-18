package com.apisports.knime.football.nodes.referencedata;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Node factory for Reference Data Loader node.
 * Loads countries, leagues, teams, and venues from API-Sports to populate UI dropdowns.
 */
public class ReferenceDataLoaderNodeFactory extends NodeFactory<ReferenceDataLoaderNodeModel> {

    @Override
    public ReferenceDataLoaderNodeModel createNodeModel() {
        return new ReferenceDataLoaderNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<ReferenceDataLoaderNodeModel> createNodeView(final int viewIndex,
                                                                  final ReferenceDataLoaderNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new ReferenceDataLoaderNodeDialog();
    }
}
