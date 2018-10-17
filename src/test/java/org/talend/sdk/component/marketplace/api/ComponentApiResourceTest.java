package org.talend.sdk.component.marketplace.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.marketplace.model.Component;
import org.talend.sdk.component.marketplace.model.Download;
import org.talend.sdk.component.marketplace.model.Vendor;
import org.talend.sdk.component.marketplace.service.dao.AuditedEntityDao;
import org.talend.sdk.component.marketplace.service.storage.CarStorage;
import org.talend.sdk.component.marketplace.test.Context;

@MonoMeecrowaveConfig
class ComponentApiResourceTest {
    @Inject
    private Context context;

    @Inject
    private AuditedEntityDao dao;

    @Inject
    private WebTarget target;

    @Inject
    private CarStorage storage;

    private Component component;
    private Download download;
    private final Collection<Runnable> onDestroy = new ArrayList<>();

    @BeforeEach
    void init() {
        final Vendor vendor = new Vendor();
        vendor.setName("machine" + System.nanoTime());

        component = new Component();
        component.setName("A Component " + System.nanoTime());
        component.setDescription("description :)");
        component.setLicense("https://www.apache.org/licenses/LICENSE-2.0");
        component.setSources("https://github.com/Talend/component-runtime");
        component.setBugtracker("https://jira.talendforge.org/projects/TCOMP/issues");
        component.setChangelog("Things can change");
        component.setDocumentation("https://talend.github.io/component-runtime/");
        component.setVendor(vendor);

        download = new Download();
        download.setName("component1-" + System.nanoTime());
        download.setComponentVersion("1.0.0");
        download.setComponent(component);

        context.executeInTx(() -> {
            dao.save(vendor);
            dao.save(component);
            dao.save(download);

            onDestroy.add(() -> storage.delete(download));
            onDestroy.add(() -> dao.deleteById(Download.class, download.getId()));
            onDestroy.add(() -> dao.deleteById(Component.class, component.getId()));
            onDestroy.add(() -> dao.deleteById(Vendor.class, vendor.getId()));
        });

        storage.save(new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)), download);
    }

    @AfterEach
    void destroy() {
        context.executeInTx(() -> onDestroy.forEach(Runnable::run));
        onDestroy.clear();
        component = null;
        download = null;
    }

    @Test
    void getComponents() {
        final ComponentApiResource.Components components = context.withToken(token ->
                target.path("v1/component/index")
                      .request(APPLICATION_JSON_TYPE)
                      .header(HttpHeaders.AUTHORIZATION, token)
                      .get(ComponentApiResource.Components.class));
        assertEquals(1, components.getTotal());
        assertEquals(1, components.getItems().size());
        assertEquals(component.getName(), components.getItems().iterator().next().getName());
    }

    @Test
    void getDownloads() {
        final ComponentApiResource.Downloads downloads = context.withToken(token ->
                target.path("v1/component/downloads/{id}")
                      .resolveTemplate("id", component.getId())
                      .request(APPLICATION_JSON_TYPE)
                      .header(HttpHeaders.AUTHORIZATION, token)
                      .get(ComponentApiResource.Downloads.class));
        assertEquals(1, downloads.getItems().size());
        assertEquals(download.getId(), downloads.getItems().iterator().next().getId());
    }

    @Test
    void getDownload() {
        assertEquals("abc", context.withToken(token ->
                target.path("v1/component/download/{id}")
                      .resolveTemplate("id", download.getId())
                      .request(APPLICATION_OCTET_STREAM_TYPE)
                      .header(HttpHeaders.AUTHORIZATION, token)
                      .get(String.class)));
    }
}
