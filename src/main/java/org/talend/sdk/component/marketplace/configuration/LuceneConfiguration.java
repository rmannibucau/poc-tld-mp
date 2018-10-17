package org.talend.sdk.component.marketplace.configuration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import lombok.Getter;

@Getter
@ApplicationScoped
public class LuceneConfiguration {
    @Inject
    @ConfigProperty(name = "talend.marketplace.lucene.directory", defaultValue = "[memory]")
    private String directory;

    @Inject
    @ConfigProperty(name = "talend.marketplace.lucene.commitInterval", defaultValue = "10")
    private Integer commitInterval;
}
