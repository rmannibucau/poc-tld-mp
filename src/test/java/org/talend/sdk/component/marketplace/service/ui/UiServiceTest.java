package org.talend.sdk.component.marketplace.service.ui;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.ServletException;

import org.apache.geronimo.microprofile.impl.jwtauth.cdi.GeronimoJwtAuthExtension;
import org.apache.geronimo.microprofile.impl.jwtauth.servlet.JwtRequest;
import org.apache.geronimo.microprofile.impl.jwtauth.servlet.MockJwtRequest;
import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.marketplace.model.Component;
import org.talend.sdk.component.marketplace.model.Vendor;
import org.talend.sdk.component.marketplace.test.Context;

@MonoMeecrowaveConfig
class UiServiceTest {
    @Inject
    private UiService service;

    @Inject
    private Context context;

    @Inject
    private EntityManager entityManager;

    @Inject
    private GeronimoJwtAuthExtension jwtAuthExtension;

    @Test
    void createForm() throws ServletException, IOException {
        final Vendor vendor = new Vendor();
        vendor.setName("Talend");
        context.executeInTx(() -> entityManager.persist(vendor));
        try {
            jwtAuthExtension.execute(new MockJwtRequest(), () -> context.execute(() -> {
                final Ui form = service.createFormFor(Component.class);
                assertNotNull(form.getUiSchema());
                assertEquals(1, form.getUiSchema()
                                    .size());
                assertUiSchema(form.getUiSchema()
                                   .iterator()
                                   .next()
                                   .getItems());

                assertNotNull(form.getJsonSchema());
                assertJsonSchema(form);
            }));
        } finally {
            context.executeInTx(() -> entityManager.remove(entityManager.getReference(Vendor.class, vendor.getId())));
        }
    }

    private void assertJsonSchema(final Ui form) {
        assertEquals(9, form.getJsonSchema().getProperties().size());
    }

    private void assertUiSchema(final Collection<UiSchema> items) {
        assertEquals(9, items.size());
        assertEquals("vendor/name/license/sources/bugtracker/documentation/description/changelog/version",
                items.stream().map(UiSchema::getKey).collect(joining("/")));

        final Collection<UiSchema.NameValue> references = items.iterator().next().getTitleMap();
        assertNotNull(references);
        assertEquals(1, references.size());
        assertEquals("Talend", references.iterator().next().getName());
    }
}
