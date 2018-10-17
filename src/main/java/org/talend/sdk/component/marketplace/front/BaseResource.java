package org.talend.sdk.component.marketplace.front;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.marketplace.cdi.Tx;
import org.talend.sdk.component.marketplace.front.model.BaseModel;
import org.talend.sdk.component.marketplace.front.model.Page;
import org.talend.sdk.component.marketplace.model.AuditedEntity;
import org.talend.sdk.component.marketplace.service.dao.AuditedEntityDao;
import org.talend.sdk.component.marketplace.service.event.DeletedEvent;
import org.talend.sdk.component.marketplace.service.event.PersistedEvent;
import org.talend.sdk.component.marketplace.service.security.User;
import org.talend.sdk.component.marketplace.service.ui.UiService;

import lombok.Data;

@ApplicationScoped
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public abstract class BaseResource<Entity extends AuditedEntity, Dto extends BaseModel> {

    protected Constructor<Entity> entityFactory;

    @Inject
    protected AuditedEntityDao auditedEntityDao;

    @Inject
    protected EntityManager entityManager;

    @Inject
    private BeanManager beanManager;

    @Inject
    protected UiService uiService;

    @Inject
    private Jsonb jsonb;

    @Inject
    protected User user;

    private Event<PersistedEvent<Entity>> persistEvent;

    private Event<DeletedEvent<Entity>> deleteEvent;

    @PostConstruct
    protected void init() {
        try {
            entityFactory = Class.class
                    .cast(ParameterizedType.class.cast(getClass().getGenericSuperclass()).getActualTypeArguments()[0])
                    .getConstructor();
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        persistEvent = getEvent(PersistedEvent.class);
        deleteEvent = getEvent(DeletedEvent.class);;
    }

    private <T> Event<T> getEvent(final Class<?> raw) {
        final Type type = new ParameterizedTypeImpl(Event.class,
                new Type[]{new ParameterizedTypeImpl(raw, new Type[]{entityFactory.getDeclaringClass()})});
        final CreationalContext<?> ctx = beanManager.createCreationalContext(null);
        return Event.class.cast(beanManager.getInjectableReference(new InjectionPointImpl(type), ctx));
    }

    // super light and fast validation but not enough
    protected void ensureLogged() {
        final Set<String> value = user.getGroups();
        if (value == null || value.isEmpty()) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
    }

    protected Entity ensureAccess(final Entity entity) {
        if (!user.getGroups().contains("**")) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        return entity;
    }

    protected void beforeFindAll() {
        // no-op
    }

    @GET
    @Path("all") // todo: use SearchEngine + propagate indexation
    public Page<Dto> findAll(@QueryParam("from") final int from, @QueryParam("max") @DefaultValue("20") final int max,
            @QueryParam("relationships") @DefaultValue("false") final boolean relationships) {
        beforeFindAll();
        final List<Dto> items = map(auditedEntityDao.findAll(entityFactory.getDeclaringClass(), from, Math.min(max, 200)),
                relationships);
        final long count = auditedEntityDao.count(entityFactory.getDeclaringClass());
        return new Page<>(items, count);
    }

    @Tx
    @POST
    public Dto create(final Dto model) {
        ensureLogged();
        try {
            final Entity entity = entityFactory.newInstance();
            map(model, entity);
            ensureAccess(entity);
            final Entity save = auditedEntityDao.save(entity);
            entityManager.flush();
            persistEvent.fire(new PersistedEvent<>(save));
            return map(save, false);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }

    @GET
    @Path("ui")
    public Ui uiCreate() {
        ensureLogged();
        final Ui form = uiService.createFormFor(entityFactory.getDeclaringClass());
        form.setProperties(new HashMap<>());
        return form;
    }

    @GET
    @Path("ui/{id}")
    public Ui uiUpdate(@PathParam("id") final String id) {
        ensureLogged();
        final Entity entity = ensureAccess(findEntityById(id));
        final Dto dto = map(entity, false);
        final Ui form = uiService.createFormFor(entityFactory.getDeclaringClass());
        form.setProperties(jsonb.fromJson(jsonb.toJson(dto), JsonObject.class));
        return form;
    }

    @Tx
    @GET
    @Path("{id}")
    public Dto find(@PathParam("id") final String id,
            @QueryParam("relationships") @DefaultValue("false") final boolean relationships) {
        return map(findEntityById(id), relationships);
    }

    @Tx
    @PUT
    @Path("{id}")
    public Dto update(@PathParam("id") final String id, final Dto model) {
        ensureLogged();
        final Entity entity = ensureAccess(findEntityById(id));
        if (entity.getVersion() != model.getVersion()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        map(model, entity);
        entityManager.flush();
        persistEvent.fire(new PersistedEvent<>(entity));
        return map(entity, false);
    }

    @Tx
    @DELETE
    @Path("{id}")
    public Dto delete(@PathParam("id") final String id) {
        ensureLogged();
        final Entity entity = ensureAccess(auditedEntityDao.deleteById(entityFactory.getDeclaringClass(), id));
        entityManager.flush();
        deleteEvent.fire(new DeletedEvent<>(entity));
        return map(entity, false);
    }

    private Entity findEntityById(final String id) {
        final Entity entity = auditedEntityDao.findById(entityFactory.getDeclaringClass(), id);
        if (entity == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return entity;
    }

    protected List<Dto> map(final List<Entity> all, final boolean withRelationships) {
        return all.stream().map(it -> map(it, withRelationships)).collect(toList());
    }

    protected abstract Dto map(Entity entity, boolean withRelationships);

    protected abstract void map(Dto model, Entity entity);

    @Data
    private static class ParameterizedTypeImpl implements ParameterizedType {

        private final Class<?> declaringClass;
        private final Type[] args;

        @Override
        public Type[] getActualTypeArguments() {
            return args;
        }

        @Override
        public Type getRawType() {
            return declaringClass;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

    @Data
    private static class InjectionPointImpl implements InjectionPoint {
        private static final Set<Annotation> QUALIFIERS = singleton(Default.Literal.INSTANCE);

        private final Type type;

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return QUALIFIERS;
        }

        @Override
        public Bean<?> getBean() {
            return null;
        }

        @Override
        public Member getMember() {
            return null;
        }

        @Override
        public Annotated getAnnotated() {
            return null;
        }

        @Override
        public boolean isDelegate() {
            return false;
        }

        @Override
        public boolean isTransient() {
            return false;
        }
    }
}
