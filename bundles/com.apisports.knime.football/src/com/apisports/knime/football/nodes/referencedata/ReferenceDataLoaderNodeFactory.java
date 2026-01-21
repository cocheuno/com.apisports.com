/*
 * Copyright 2025 Carone Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
