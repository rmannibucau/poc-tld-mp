package org.talend.sdk.component.marketplace.configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class MutableLocalConfigSource implements ConfigSource {
    private final Map<String, String> properties = new ConcurrentHashMap<>();

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public String getValue(final String propertyName) {
        return getProperties().get(propertyName);
    }

    @Override
    public String getName() {
        return "talend-marketplace-local-source";
    }
}
