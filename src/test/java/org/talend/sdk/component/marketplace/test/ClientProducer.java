package org.talend.sdk.component.marketplace.test;

import static java.lang.String.format;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.cxf.feature.LoggingFeature;
import org.apache.meecrowave.Meecrowave;

@ApplicationScoped
public class ClientProducer {
    @Produces
    public WebTarget target(final Client client, final Meecrowave.Builder config) {
        return client.target(format("http://localhost:%d/api", config.getHttpPort()));
    }

    @Produces
    public Client client() {
        return ClientBuilder.newClient().register(new LoggingFeature());
    }

    public void releaseClient(@Disposes final Client client) {
        client.close();
    }
}
