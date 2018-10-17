package org.talend.sdk.component.marketplace.configuration;

import static java.util.Arrays.asList;
import static javax.persistence.SharedCacheMode.ENABLE_SELECTIVE;
import static javax.persistence.ValidationMode.NONE;
import static javax.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.talend.sdk.component.marketplace.cdi.EntitiesModel;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class EntityManagerProducer {

    @Produces
    @ApplicationScoped
    EntityManagerFactory entityManagerFactory(final MarketplacePersistenceConfiguration configuration,
                                              final DataSource dataSource, final EntitiesModel entitiesModel) {
        final PersistenceProvider provider = ServiceLoader.load(PersistenceProvider.class)
                                                      .iterator()
                                                      .next();
        return provider.createContainerEntityManagerFactory(PersistenceUnitInfoImpl
            .builder()
            .persistenceXMLSchemaVersion("2.0")
            .persistenceUnitName("components-marketplace")
            .transactionType(RESOURCE_LOCAL)
            .jtaDataSource(dataSource)
            .nonJtaDataSource(dataSource)
            .sharedCacheMode(ENABLE_SELECTIVE)
            .validationMode(NONE)
            .classLoader(Thread.currentThread().getContextClassLoader())
            .managedClassNames(entitiesModel.getEntities())
            .excludeUnlistedClasses(true)
            .properties(asProperties(configuration.getUnitProperties().orElse("")))
            .build(), new HashMap<>());
    }

    void releaseEntityManagerFactory(@Disposes final EntityManagerFactory factory) {
        factory.close();
    }

    @Produces
    @RequestScoped
    EntityManager createEntityManager(final EntityManagerFactory entityManagerFactory) {
        return entityManagerFactory.createEntityManager();
    }

    void closeEntityManager(@Disposes EntityManager entityManager) {
        if (entityManager.isOpen()) {
            entityManager.close();
        }
    }

    @Produces
    @ApplicationScoped
    DataSource database(final MarketplacePersistenceConfiguration configuration) {
        final BasicDataSource source = new BasicDataSource();
        source.setDriverClassLoader(Thread.currentThread().getContextClassLoader());
        source.setDriverClassName(configuration.getDriver());
        source.setUrl(configuration.getUrl());
        source.setUsername(configuration.getUsername());
        source.setPassword(configuration.getPassword());
        source.setMinEvictableIdleTimeMillis(configuration.getMinEvictableIdleTimeMillis());
        source.setMinIdle(configuration.getMinIdle());
        source.setMaxIdle(configuration.getMaxIdle());
        source.setMaxTotal(configuration.getMaxTotal());
        source.setInitialSize(configuration.getInitialSize());
        source.setMaxWaitMillis(configuration.getMaxWaitMillis());
        configuration.getDefaultQueryTimeout().ifPresent(source::setDefaultQueryTimeout);
        source.setJmxName(configuration.getJmxName());
        source.setPoolPreparedStatements(Boolean.parseBoolean(configuration.getPoolPreparedStatement()));
        configuration.getTestOnBorrow().ifPresent(source::setTestOnBorrow);
        configuration.getTestOnCreate().ifPresent(source::setTestOnCreate);
        configuration.getTestOnReturn().ifPresent(source::setTestOnReturn);
        configuration.getValidationQuery().ifPresent(source::setValidationQuery);
        configuration.getValidationQueryTimeout().ifPresent(source::setValidationQueryTimeout);
        source.setFastFailValidation(configuration.isFailFastValidation());
        configuration.getInitSql().map(s -> asList(s.split("\n"))).ifPresent(source::setConnectionInitSqls);
        configuration.getConnectionProperties().ifPresent(source::setConnectionProperties);
        configuration.getDefaultSchema().ifPresent(source::setDefaultSchema);
        configuration.getDefaultCatalog().ifPresent(source::setDefaultCatalog);
        return source;
    }

    @Getter
    @Builder
    public static class PersistenceUnitInfoImpl implements PersistenceUnitInfo {

        private String persistenceXMLSchemaVersion;

        private String persistenceUnitName;

        private String persistenceProviderClassName;

        private PersistenceUnitTransactionType transactionType;

        private DataSource jtaDataSource;

        private DataSource nonJtaDataSource;

        private List<String> managedClassNames;

        private List<String> mappingFileNames;

        private List<URL> jarFileUrls;

        private URL persistenceUnitRootUrl;

        private boolean excludeUnlistedClasses;

        private SharedCacheMode sharedCacheMode;

        private ValidationMode validationMode;

        private Properties properties;

        private ClassLoader classLoader;

        private ClassLoader newTempClassLoader;

        private final Collection<ClassTransformer> transformers = new ArrayList<>();

        @Override
        public boolean excludeUnlistedClasses() {
            return excludeUnlistedClasses;
        }

        @Override
        public void addTransformer(final ClassTransformer transformer) {
            transformers.add(transformer);
        }
    }

    private Properties asProperties(final String value) {
        final Properties properties = new Properties();
        try (final Reader reader = new StringReader(value)) {
            properties.load(reader);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return properties;
    }
}
