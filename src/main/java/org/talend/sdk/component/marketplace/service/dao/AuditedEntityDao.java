package org.talend.sdk.component.marketplace.service.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.talend.sdk.component.marketplace.model.AuditedEntity;

@ApplicationScoped
public class AuditedEntityDao {

    @Inject
    private EntityManager entityManager;

    // todo: security, if user is "public" then use .findAllPublic
    public <Entity extends AuditedEntity> List<Entity> findAll(final Class<Entity> entityType, final int from, final int max) {
        return entityManager.createNamedQuery(entityType.getSimpleName() + ".findAll", entityType)
                            .setFirstResult(from)
                            .setMaxResults(max)
                            .getResultList();
    }

    public <Entity extends AuditedEntity> Entity save(final Entity entity) {
        entityManager.persist(entity);
        return entity;
    }

    public <Entity extends AuditedEntity> long count(final Class<Entity> entityType) {
        return entityManager.createNamedQuery(entityType.getSimpleName() + ".count", Number.class)
                            .getSingleResult()
                            .longValue();
    }

    public <Entity extends AuditedEntity> Entity findById(final Class<Entity> entityType, final String id) {
        return entityManager.find(entityType, id);
    }

    public <Entity extends AuditedEntity> Entity deleteById(final Class<Entity> entityClass, final String id) {
        final Entity byId = findById(entityClass, id);
        entityManager.remove(byId);
        return byId;
    }
}
