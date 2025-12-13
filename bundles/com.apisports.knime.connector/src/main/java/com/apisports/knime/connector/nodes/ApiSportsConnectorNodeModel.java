package com.apisports.knime.connector.nodes;

import com.apisports.knime.core.client.ApiSportsHttpClient;
import com.apisports.knime.core.model.Sport;
import com.apisports.knime.port.ApiSportsConnectionPortObject;
import com.apisports.knime.port.ApiSportsConnectionPortObjectSpec;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import com.apisports.knime.core.ratelimit.RateLimiterManager;
import com.apisports.knime.core.cache.CacheManager;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import java.io.File;
import java.io.IOException;

/**
 * NodeModel for the API-Sports Connector node.
 * Creates a connection to the API-Sports service.
 */
public class ApiSportsConnectorNodeModel extends NodeModel {

    static final String CFGKEY_API_KEY = "apiKey";
    static final String CFGKEY_SPORT = "sport";
    static final String CFGKEY_TIER = "tier";
    
    private final SettingsModelString m_apiKey = new SettingsModelString(CFGKEY_API_KEY, "");
    private final SettingsModelString m_sport = new SettingsModelString(CFGKEY_SPORT, Sport.FOOTBALL.getId());
    private final SettingsModelString m_tier = new SettingsModelString(CFGKEY_TIER, "free");

    protected ApiSportsConnectorNodeModel() {
        super(new PortType[0], new PortType[]{ApiSportsConnectionPortObject.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        String apiKey = m_apiKey.getStringValue();
        Sport sport = Sport.fromId(m_sport.getStringValue());
        String tier = m_tier.getStringValue();
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new InvalidSettingsException("API key must not be empty");
        }
        
        RateLimiterManager rateLimiter = new RateLimiterManager();
        CacheManager cacheManager = new CacheManager();
        ApiSportsHttpClient client = new ApiSportsHttpClient(apiKey, sport, rateLimiter, cacheManager);
        String apiKeyHash = Integer.toHexString(apiKey.hashCode());
        ApiSportsConnectionPortObjectSpec spec = new ApiSportsConnectionPortObjectSpec(sport, apiKeyHash, tier);
        ApiSportsConnectionPortObject portObject = new ApiSportsConnectionPortObject(spec, client);
        
        return new PortObject[]{portObject};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        String apiKey = m_apiKey.getStringValue();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new InvalidSettingsException("API key must be configured");
        }
        
        Sport sport = Sport.fromId(m_sport.getStringValue());
        String tier = m_tier.getStringValue();
        
        String apiKeyHash = Integer.toHexString(apiKey.hashCode());
        return new PortObjectSpec[]{new ApiSportsConnectionPortObjectSpec(sport, apiKeyHash, tier)};
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_apiKey.saveSettingsTo(settings);
        m_sport.saveSettingsTo(settings);
        m_tier.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_apiKey.loadSettingsFrom(settings);
        m_sport.loadSettingsFrom(settings);
        m_tier.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_apiKey.validateSettings(settings);
        m_sport.validateSettings(settings);
        m_tier.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException {
        // No internals to load
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException {
        // No internals to save
    }

    @Override
    protected void reset() {
        // Nothing to reset
    }
}
