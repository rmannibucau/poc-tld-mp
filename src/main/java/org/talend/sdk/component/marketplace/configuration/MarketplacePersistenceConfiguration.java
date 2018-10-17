package org.talend.sdk.component.marketplace.configuration;

import static lombok.AccessLevel.NONE;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import lombok.Data;
import lombok.Setter;

@Data
@Setter(NONE)
@ApplicationScoped
public class MarketplacePersistenceConfiguration {

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.url")
    private String url;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.driver")
    private String driver;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.username")
    private String username;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.password")
    private String password;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.initSql")
    private Optional<String> initSql;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.connectionProperties")
    private Optional<String> connectionProperties;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.defaultSchema")
    private Optional<String> defaultSchema;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.defaultCatalog")
    private Optional<String> defaultCatalog;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.initialSize", defaultValue = "1")
    private int initialSize;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.maxTotal", defaultValue = "64")
    private int maxTotal;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.minIdle", defaultValue = "1")
    private int minIdle;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.maxIdle", defaultValue = "8")
    private int maxIdle;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.minEvictableIdleTimeMillis", defaultValue = "1800000")
    private long minEvictableIdleTimeMillis;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.validationQuery")
    private Optional<String> validationQuery;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.failFastValidation", defaultValue = "true")
    private boolean failFastValidation;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.testOnBorrow")
    private Optional<Boolean> testOnBorrow;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.testOnCreate")
    private Optional<Boolean> testOnCreate;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.testOnReturn")
    private Optional<Boolean> testOnReturn;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.validationQueryTimeout")
    private Optional<Integer> validationQueryTimeout;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.maxWaitMillis", defaultValue = "-1")
    private long maxWaitMillis;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.defaultQueryTimeout")
    private Optional<Integer> defaultQueryTimeout;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.jmxName", defaultValue = "talend:type=database,application=marketplace,name=components-marketplace")
    private String jmxName;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.database.poolPreparedStatement", defaultValue = "true")
    private String poolPreparedStatement;

    @Inject
    @ConfigProperty(name = "talend.marketplace.persistence.unit.properties", defaultValue = "openjpa.Log=slf4j\nopenjpa.RuntimeUnenhancedClasses=unsupported")
    private Optional<String> unitProperties;
}
