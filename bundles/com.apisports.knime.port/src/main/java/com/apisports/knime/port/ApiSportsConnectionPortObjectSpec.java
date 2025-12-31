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

package com.apisports.knime.port;

import com.apisports.knime.core.model.Sport;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.JComponent;
import java.util.Objects;

/**
 * Specification for API-Sports connection port object.
 * Contains connection configuration without the actual client instance.
 */
public class ApiSportsConnectionPortObjectSpec implements PortObjectSpec {
    
    private final Sport sport;
    private final String apiKeyHash; // Store hash instead of actual key for security
    private final String tierName;

    public ApiSportsConnectionPortObjectSpec(Sport sport, String apiKeyHash, String tierName) {
        this.sport = Objects.requireNonNull(sport, "Sport cannot be null");
        this.apiKeyHash = Objects.requireNonNull(apiKeyHash, "API key hash cannot be null");
        this.tierName = Objects.requireNonNull(tierName, "Tier name cannot be null");
    }

    public Sport getSport() {
        return sport;
    }

    public String getApiKeyHash() {
        return apiKeyHash;
    }

    public String getTierName() {
        return tierName;
    }

    @Override
    public JComponent[] getViews() {
        // Return null for now - could add a view showing connection details
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ApiSportsConnectionPortObjectSpec)) {
            return false;
        }
        ApiSportsConnectionPortObjectSpec other = (ApiSportsConnectionPortObjectSpec) obj;
        return sport == other.sport 
            && apiKeyHash.equals(other.apiKeyHash)
            && tierName.equals(other.tierName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sport, apiKeyHash, tierName);
    }

    @Override
    public String toString() {
        return String.format("ApiSportsConnection[sport=%s, tier=%s]", sport.getDisplayName(), tierName);
    }
}
