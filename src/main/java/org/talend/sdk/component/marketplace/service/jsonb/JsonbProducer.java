package org.talend.sdk.component.marketplace.service.jsonb;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class JsonbProducer {
    @Produces
    @ApplicationScoped
    Jsonb jsonb() {
        return JsonbBuilder.create();
    }

    void releaseJsonb(@Disposes final Jsonb jsonb) {
        try {
            jsonb.close();
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
    }
}
