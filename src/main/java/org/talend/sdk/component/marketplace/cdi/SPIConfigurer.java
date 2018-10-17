package org.talend.sdk.component.marketplace.cdi;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.talend.sdk.component.marketplace.service.search.SearchEngine;
import org.talend.sdk.component.marketplace.service.storage.CarStorage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SPIConfigurer implements Extension {
    private final Config config = ConfigProvider.getConfig();
    private String storageClass;
    private String indexClass;

    void init(@Observes final BeforeBeanDiscovery beforeBeanDiscovery) {
        storageClass = config.getOptionalValue("talend.marketplace.storage.car.type", String.class)
                .orElse("org.talend.sdk.component.marketplace.service.storage.FilesystemStorage");
        indexClass = config.getOptionalValue("talend.marketplace.indexation.type", String.class)
                .orElse("org.talend.sdk.component.marketplace.service.search.LuceneSearchEngine");
    }

    void vetoUnNeededStorages(@Observes final ProcessAnnotatedType<? extends CarStorage> storage) {
        if (!storage.getAnnotatedType().getJavaClass().getName().equals(storageClass)) {
            storage.veto();
        }
    }

    void vetoUnNeededEngines(@Observes final ProcessAnnotatedType<? extends SearchEngine> engine) {
        if (!engine.getAnnotatedType().getJavaClass().getName().equals(indexClass)) {
            engine.veto();
        }
    }
}
