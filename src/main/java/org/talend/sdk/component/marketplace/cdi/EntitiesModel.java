package org.talend.sdk.component.marketplace.cdi;

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.talend.sdk.component.marketplace.model.AuditedEntity;
import org.talend.sdk.component.marketplace.service.ui.View;

import lombok.Getter;

public class EntitiesModel implements Extension {
    @Getter
    private final List<String> entities = new ArrayList<>();

    @Getter
    private final Map<Class<?>, Class<? extends AuditedEntity>> modelMapping = new HashMap<>();

    void captureEntities(@Observes @WithAnnotations(Entity.class) final ProcessAnnotatedType<?> pat) {
        Class<?> current = pat.getAnnotatedType().getJavaClass();
        while (current != Object.class &&
                (current.isAnnotationPresent(Entity.class) || current.isAnnotationPresent(MappedSuperclass.class))) {
            entities.add(current.getName());
            current = current.getSuperclass();
        }
    }

    void mapEntities(@Observes @WithAnnotations(Entity.class) final ProcessAnnotatedType<? extends AuditedEntity> pat) {
        final Class<? extends AuditedEntity> javaClass = pat.getAnnotatedType().getJavaClass();
        ofNullable(javaClass.getAnnotation(View.class))
                .map(View::value)
                .ifPresent(model -> modelMapping.put(model, javaClass));
    }
}
