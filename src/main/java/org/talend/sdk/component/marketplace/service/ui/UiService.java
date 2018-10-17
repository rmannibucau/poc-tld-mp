package org.talend.sdk.component.marketplace.service.ui;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparingInt;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Id;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.marketplace.cdi.EntitiesModel;
import org.talend.sdk.component.marketplace.model.AuditedEntity;
import org.talend.sdk.component.marketplace.model.Component;
import org.talend.sdk.component.marketplace.model.Vendor;
import org.talend.sdk.component.marketplace.service.dao.AuditedEntityDao;
import org.talend.sdk.component.marketplace.service.security.User;

import lombok.Data;

@ApplicationScoped
public class UiService {

    private final ConcurrentMap<Key, Ui> forms = new ConcurrentHashMap<>();

    @Inject
    private EntitiesModel entitiesModel;

    @Inject
    private AuditedEntityDao dao;

    @Inject
    private EntityManager entityManager;

    @Inject
    @ConfigProperty(name = "talend.marketplace.uispec.datalist.proposals.max", defaultValue = "1000")
    private Integer maxItemsInDataList;

    @Inject
    private User user;

    // executed in a logged context
    public Ui createFormFor(final Class<?> clazz) {
        final Ui model = forms.computeIfAbsent(new Key(clazz, user.getGroups().contains("**")),
                key -> {
                    if (clazz.isAnnotationPresent(View.class)) {
                        return generateUi(clazz.getAnnotation(View.class).value());
                    }
                    return generateUi(clazz);
                });
        return Ui.ui()
                 .withJsonSchema(model.getJsonSchema())
                 .withUiSchema(model.getUiSchema())
                 .withProperties(Collections.emptyMap())
                 .build();
    }

    private Ui generateUi(final Class<?> clazz) {
        final Ui ui = new Ui();
        ui.setJsonSchema(generateJsonSchema(clazz, clazz, null, null));
        ui.setUiSchema(singletonList(generateUiSchemas("", clazz, clazz)));
        return ui;
    }

    private UiSchema generateUiSchemas(final String keyPrefix, final AnnotatedElement element, final Class<?> clazz) {
        if (clazz == boolean.class) {
            final UiSchema.Builder builder = UiSchema.uiSchema();
            builder.withKey(keyPrefix);
            builder.withWidget("checkbox");
            applyConfig(element, builder);
            return builder.build();
        }
        if (isText(clazz) || hasReference(element)) {
            final UiSchema.Builder builder = UiSchema.uiSchema();
            builder.withKey(keyPrefix);
            builder.withWidget("text");
            if (element.isAnnotationPresent(Id.class) || clazz == Date.class /*audit*/) {
                builder.withReadOnly(true);
            }
            applyConfig(element, builder);
            return builder.build();
        }
        if (isNumber(clazz)) {
            final UiSchema.Builder builder = UiSchema.uiSchema();
            builder.withKey(keyPrefix);
            builder.withWidget("text");
            builder.withType("number");
            applyConfig(element, builder);
            return builder.build();
        }
        if (clazz.isPrimitive()) {
            throw new IllegalArgumentException("Unsupported (yet) type: " + clazz);
        }

        final List<UiSchema> properties = new ArrayList<>();
        final Map<String, Integer> positions = new HashMap<>();
        final UiSchema.Builder builder = UiSchema.uiSchema()
                                                  .withKey(keyPrefix.isEmpty() ? null : keyPrefix)
                                                  .withWidget("fieldset");

        Class<?> current = clazz;
        while (current != Object.class && current != null) {
            properties.addAll(Stream.of(current.getDeclaredFields())
                                    .filter(this::isIncluded)
                                    .map(it -> {
                                        final String nextKey = keyPrefix + (keyPrefix.isEmpty() ? "" : ".") + it.getName();
                                        final int pos = ofNullable(it.getAnnotation(View.Schema.class)).map(View.Schema::position).orElse(-1);
                                        positions.put(it.getName(), pos < 0 ? Integer.MAX_VALUE : pos);
                                        return generateUiSchemas(nextKey, it, it.getType());
                                    })
                                    .collect(toList()));
            current = current.getSuperclass();
        }
        return applyConfig(element, builder)
                .withItems(ofNullable(element.getAnnotation(View.Schema.class))
                        .map(View.Schema::order)
                        .filter(order -> order.length > 0)
                        .map(order -> {
                            final List<String> orderIdx = asList(order);
                            properties.sort(comparingInt((final UiSchema a) -> {
                                final int idx = orderIdx.indexOf(keyToName(a.getKey()));
                                return idx < 0 ? Integer.MAX_VALUE : idx;
                            }).thenComparing(UiSchema::getKey));
                            return properties;
                        }).orElseGet(() -> {
                            properties.sort(comparingInt((final UiSchema a) -> positions.get(keyToName(a.getKey())))
                                              .thenComparing(UiSchema::getKey));
                            return properties;
                        }))
                .build();
    }

    private Boolean hasReference(final AnnotatedElement element) {
        return ofNullable(element.getAnnotation(View.Schema.class))
                .map(View.Schema::reference)
                .map(it -> it != View.Schema.class)
                .orElse(false);
    }

    private String keyToName(final String key) {
        return key.substring(key.lastIndexOf('.') + 1);
    }

    private UiSchema.Builder applyConfig(final AnnotatedElement element,
                                         final UiSchema.Builder builder) {
        ofNullable(element.getAnnotation(View.Schema.class)).ifPresent(config -> {
            final String type = config.type();
            if (!type.isEmpty()) {
                builder.withType(type);
            }

            final String title = config.title();
            if (!title.isEmpty()) {
                builder.withTitle(title);
            }

            final String widget = config.widget();
            if (!widget.isEmpty()) {
                builder.withWidget(widget);
            }

            if (config.readOnly()) {
                builder.withReadOnly(true);
            }

            final Class<?> refModel = config.reference();
            if (refModel != View.Schema.class) {
                final Class<? extends AuditedEntity> entity = entitiesModel.getModelMapping()
                   .entrySet()
                   .stream()
                   .filter(it -> it.getKey().equals(refModel) || it.getValue().equals(refModel))
                   .findFirst()
                   .map(Map.Entry::getValue)
                   .orElseThrow(() -> new IllegalArgumentException("No entity matching model " + refModel));

                if (widget.isEmpty()) {
                    builder.withWidget("datalist");
                }
                builder.withTitleMap(createTitleMap(entity));
            }
        });
        return builder;
    }

    private List<UiSchema.NameValue> createTitleMap(final Class<? extends AuditedEntity> entity) {
        final Set<String> groups = user.getGroups();
        if (entity == Vendor.class && !groups.contains("**")) {
            return toTitleMap(groups.stream()
                                  .map(it -> dao.findById(entity, it))
                                  .filter(Objects::nonNull));
        }
        if (entity == Component.class && !groups.contains("**")) {
            return toTitleMap(groups.stream()
                                  .map(it -> dao.findById(entity, it))
                                  .filter(Objects::nonNull)
                                  .findFirst()
                                  .map(vendor -> entityManager.createNamedQuery("Component.findAllByVendor", Component.class)
                                    .setParameter("vendor", vendor)
                                    .getResultList())
                                  .orElseGet(Collections::emptyList)
                                  .stream());
        }
        return toTitleMap(dao.findAll(entity, 0, maxItemsInDataList).stream());
    }

    private List<UiSchema.NameValue> toTitleMap(final Stream<? extends AuditedEntity> all) {
        return all.map(item -> new UiSchema.NameValue.Builder().withValue(item.getId()).withName(item.getName()).build())
                  .collect(toList());
    }

    private JsonSchema generateJsonSchema(final AnnotatedElement element, final Class<?> clazz,
                                          final JsonSchema parent, final String name) {
        if (clazz == boolean.class) {
            final JsonSchema.Builder builder = JsonSchema.jsonSchema();
            Stream.of(element.getAnnotationsByType(View.Schema.class))
                  .forEach(schema -> applyConfig(parent, name, builder, schema));
            return builder.withType("boolean").withDefaultValue(false).build();
        }
        if (isText(clazz)) {
            final JsonSchema.Builder builder = JsonSchema.jsonSchema();
            Stream.of(element.getAnnotationsByType(View.Schema.class))
                  .forEach(schema -> applyConfig(parent, name, builder, schema));
            return builder.withType("string").build();
        }
        if (isNumber(clazz)) {
            final JsonSchema.Builder builder = JsonSchema.jsonSchema().withType("number");
            ofNullable(element.getAnnotation(View.Schema.class)).ifPresent(schema -> applyConfig(parent, name, builder, schema));
            return builder.build();
        }
        if (clazz.isPrimitive()) {
            throw new IllegalArgumentException("Unsupported (yet) type: " + clazz);
        }

        final JsonSchema.Builder builder = JsonSchema.jsonSchema().withProperties(new HashMap<>());
        ofNullable(element.getAnnotation(View.Schema.class)).ifPresent(schema -> applyConfig(parent, name, builder, schema));
        final JsonSchema schema = builder.build();
        final Map<String, JsonSchema> properties = schema.getProperties();

        Class<?> current = clazz;
        while (current != Object.class && current != null) {
            properties.putAll(Stream.of(current.getDeclaredFields())
                  .filter(this::isIncluded)
                  .collect(toMap(Field::getName, it -> generateJsonSchema(it, it.getType(), schema, it.getName()))));
            current = current.getSuperclass();
        }
        return schema;
    }

    private void applyConfig(final JsonSchema parent, final String name, JsonSchema.Builder builder,
                             final View.Schema schema) {
        final int maxLength = schema.length();
        if (maxLength >= 0) {
            builder.withMaxLength(maxLength);
        }

        if (schema.required() && parent != null) {
            if (parent.getRequired() == null) {
                parent.setRequired(new ArrayList<>());
            }
            parent.getRequired().add(name);
        }

        final String pattern = schema.pattern();
        if (!pattern.isEmpty()) {
            builder.withPattern(pattern);
        }
    }

    private boolean isNumber(final Class<?> clazz) {
        return clazz == long.class || clazz == int.class;
    }

    private boolean isText(final Class<?> clazz) {
        return clazz == String.class || clazz == Date.class;
    }

    private boolean isIncluded(final Field field) {
        // parameterized type are relationships -> specific pages
        return !field.isAnnotationPresent(View.Skip.class) && Class.class.isInstance(field.getGenericType());
    }

    @Data
    private static class Key {
        private final Class<?> model;
        private final boolean sudo;
    }
}
