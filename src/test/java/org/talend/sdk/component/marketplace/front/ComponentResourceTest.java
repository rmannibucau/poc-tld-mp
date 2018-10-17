package org.talend.sdk.component.marketplace.front;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.marketplace.front.model.ComponentModel;
import org.talend.sdk.component.marketplace.front.model.DownloadModel;
import org.talend.sdk.component.marketplace.front.model.Page;
import org.talend.sdk.component.marketplace.model.Component;
import org.talend.sdk.component.marketplace.model.Download;
import org.talend.sdk.component.marketplace.model.Vendor;
import org.talend.sdk.component.marketplace.service.dao.AuditedEntityDao;
import org.talend.sdk.component.marketplace.service.event.DeletedEvent;
import org.talend.sdk.component.marketplace.service.storage.CarStorage;
import org.talend.sdk.component.marketplace.test.Context;

@MonoMeecrowaveConfig
class ComponentResourceTest {

    @Inject
    private Context context;

    @Inject
    private AuditedEntityDao dao;

    @Inject
    private WebTarget target;

    @Inject
    private CarStorage carStorage;

    @Inject
    private EntityManager entityManager;

    @Inject
    private Event<DeletedEvent<Component>> deletedEventEvent;

    @Test
    void downloadUi() {
        final Vendor vendor = newVendor();
        final Component component = newComponent(vendor);
        context.executeInTx(() -> {
            dao.save(vendor);
            dao.save(component);
        });

        final Ui ui = context.withToken(vendor, token -> target.path("component/ui/download")
                                                    .request(APPLICATION_JSON_TYPE)
                                                    .header(HttpHeaders.AUTHORIZATION, token)
                                                    .get(Ui.class));
        assertEquals(1, ui.getUiSchema().size());
        final Collection<UiSchema> items = ui.getUiSchema().iterator().next().getItems();
        assertEquals(4, items.size()); // version, compVersion, compId

        final Map<String, String> map = items.stream()
                                                 .filter(it -> it.getKey().equals("componentId"))
                                                 .findFirst()
                                                 .orElseThrow(() -> new IllegalArgumentException("No componentId"))
                                                 .getTitleMap()
                                                 .stream()
                                                 .collect(toMap(UiSchema.NameValue::getValue, UiSchema.NameValue::getName));
        assertEquals(singletonMap(component.getId(), "test-component"), map);

        context.executeInTx(() -> {
            dao.deleteById(Component.class, component.getId());
            dao.deleteById(Vendor.class, vendor.getId());
        });
    }

    @Test
    void download(final TestInfo info) throws IOException {
        final Vendor vendor = newVendor();
        final Component component = newComponent(vendor);
        context.executeInTx(() -> {
            dao.save(vendor);
            dao.save(component);
        });
        final File car = createCar(info);

        final DownloadModel download;
        try (final InputStream src = new FileInputStream(car)) {
            download = context.withToken(token -> target.path("component/download/{componentId}")
                 .resolveTemplate("componentId", component.getId())
                 .queryParam("downloadName", "test-1.2.3.car")
                 .queryParam("componentVersion", "1.2.3")
                 .request(APPLICATION_JSON_TYPE)
                 .header(HttpHeaders.AUTHORIZATION, token)
                 .post(entity(new Attachment("file", src, new ContentDisposition("form-data;name=file;filename=" + car.getName())), MULTIPART_FORM_DATA_TYPE), DownloadModel.class));
        } finally {
            car.delete();
        }
        final byte[] carBytes = target.path("component/download/{id}")
              .resolveTemplate("id", download.getId())
              .request(APPLICATION_OCTET_STREAM_TYPE)
              .get(byte[].class);
        try (final JarInputStream retrieve = new JarInputStream(new ByteArrayInputStream(carBytes))) {
            assertEquals("TALEND-INF/metadata.properties", retrieve.getNextJarEntry().getName());
        } catch (final IOException e) {
            fail(e.getMessage());
        }

        context.execute(() -> carStorage.delete(entityManager.find(Download.class, download.getId())));
        context.executeInTx(() -> {
            dao.deleteById(Download.class, download.getId());
            dao.deleteById(Component.class, component.getId());
            dao.deleteById(Vendor.class, vendor.getId());
        });
    }

    @Test
    void attachDownload(final TestInfo info) throws IOException {
        final Vendor vendor = newVendor();
        final Component component = newComponent(vendor);
        context.executeInTx(() -> {
            dao.save(vendor);
            dao.save(component);
        });
        final File car = createCar(info);

        final DownloadModel download;
        try (final InputStream src = new FileInputStream(car)) {
            download = context.withToken(token -> target.path("component/download/{componentId}")
                 .resolveTemplate("componentId", component.getId())
                 .queryParam("downloadName", "test-1.2.3.car")
                 .queryParam("componentVersion", "1.2.3")
                 .request(APPLICATION_JSON_TYPE)
                 .header(HttpHeaders.AUTHORIZATION, token)
                 .post(entity(new Attachment("file", src, new ContentDisposition("form-data;name=file;filename=" + car.getName())), MULTIPART_FORM_DATA_TYPE), DownloadModel.class));
        } finally {
            car.delete();
        }
        assertNotNull(download);
        assertNotNull(download.getId());
        assertEquals("1.2.3", download.getComponentVersion());
        context.execute(() -> {
            try (final JarInputStream retrieve = new JarInputStream(carStorage.retrieve(entityManager.find(Download.class, download.getId())))) {
                assertEquals("TALEND-INF/metadata.properties", retrieve.getNextJarEntry().getName());
            } catch (final IOException e) {
                fail(e.getMessage());
            }
        });

        context.execute(() -> carStorage.delete(entityManager.find(Download.class, download.getId())));
        context.executeInTx(() -> {
            dao.deleteById(Download.class, download.getId());
            dao.deleteById(Component.class, component.getId());
            dao.deleteById(Vendor.class, vendor.getId());
        });
    }

    @Test
    void removeDownload(final TestInfo info) throws IOException {
        final Vendor vendor = newVendor();
        final Component component = newComponent(vendor);
        context.executeInTx(() -> {
            dao.save(vendor);
            dao.save(component);
        });
        final File car = createCar(info);

        final DownloadModel download;
        try (final InputStream src = new FileInputStream(car)) {
            download = context.withToken(token -> target.path("component/download/{componentId}")
                 .resolveTemplate("componentId", component.getId())
                 .queryParam("downloadName", "test-1.2.3.car")
                 .queryParam("componentVersion", "1.2.3")
                 .request(APPLICATION_JSON_TYPE)
                 .header(HttpHeaders.AUTHORIZATION, token)
                 .post(entity(new Attachment("file", src, new ContentDisposition("form-data;name=file;filename=" + car.getName())), MULTIPART_FORM_DATA_TYPE), DownloadModel.class));
        } finally {
            car.delete();
        }

        final Supplier<ComponentModel> componentLoader = () -> target.path("component/{id}")
                                                                 .resolveTemplate("id", component.getId())
                                                                 .queryParam("relationships", true)
                                                                 .request(APPLICATION_JSON_TYPE)
                                                                 .get(ComponentModel.class);

        // ensure the download is listed in the component model with relationships
        final ComponentModel found = componentLoader.get();
        assertEquals(singletonList(download.getId()), found.getDownloads().stream().map(DownloadModel::getId).collect(toList()));

        // then drop it
        context.withToken(token -> {
            assertEquals(
                Response.Status.OK.getStatusCode(),
                target.path("component/download/{componentId}/{downloadId}")
                    .resolveTemplate("componentId", component.getId())
                    .resolveTemplate("downloadId", found.getDownloads().iterator().next().getId())
                    .request(APPLICATION_JSON_TYPE)
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .delete().getStatus());
            return null;
        });

        assertTrue(componentLoader.get().getDownloads().isEmpty());

        context.executeInTx(() -> {
            dao.deleteById(Component.class, component.getId());
            dao.deleteById(Vendor.class, vendor.getId());
        });
    }

    @Test
    void findAll() {
        final Vendor vendor = newVendor();
        context.executeInTx(() -> {
            dao.save(vendor);

            final Component entity = newComponent(vendor);
            dao.save(entity);
        });
        final Page<ComponentModel> components = target.path("component/all").request(APPLICATION_JSON_TYPE)
                .get(ComponentModel.Page.class);
        context.executeInTx(() -> {
            dao.deleteById(Vendor.class, vendor.getId());
            dao.deleteById(Component.class, components.getItems().iterator().next().getId());
        });

        assertEquals(1, components.getItems().size());
        assertEquals(1, components.getTotal());

        final ComponentModel model = components.getItems().iterator().next();
        assertNotNull(model.getId());
        assertNotNull(model.getCreated());
        assertNotNull(model.getUpdated());
        assertEquals("test-component", model.getName());
        assertEquals("Super component", model.getDescription());
        assertNull(model.getProducts());
    }

    @Test
    void save() {
        final Vendor vendor = newVendor();
        context.executeInTx(() -> dao.save(vendor));

        final ComponentModel source = new ComponentModel();
        source.setName("save");
        source.setDescription("desc");
        source.setLicense("https://www.apache.org/licenses/LICENSE-2.0");
        source.setSources("https://github.com/Talend/component-runtime");
        source.setBugtracker("https://jira.talendforge.org/projects/TCOMP/issues");
        source.setChangelog("Things can change");
        source.setDocumentation("https://talend.github.io/component-runtime/");
        source.setVendor(vendor.getId());
        final ComponentModel created = context.withToken(token -> target.path("component")
                                             .request(APPLICATION_JSON_TYPE)
                                             .header(HttpHeaders.AUTHORIZATION, token)
                                             .post(entity(source, APPLICATION_JSON_TYPE),
                                                       ComponentModel.class));
        context.executeInTx(() -> dao.deleteById(Vendor.class, vendor.getId()));

        assertNotNull(created.getId());
        assertEquals(source.getName(), created.getName());
        assertEquals(source.getDescription(), created.getDescription());
        context.executeInTx(() -> {
            final Component byId = dao.findById(Component.class, created.getId());
            assertNotNull(byId);
            assertEquals(created.getName(), byId.getName());
            assertEquals(created.getDescription(), byId.getDescription());
            deletedEventEvent.fire(new DeletedEvent<>(byId));
            dao.deleteById(Component.class, created.getId());
        });
    }

    @Test
    void findById() {
        final Component entity = new Component();
        context.executeInTx(() -> {
            final Vendor vendor = newVendor();
            dao.save(vendor);

            entity.setName("test-component");
            entity.setDescription("Super component");
            entity.setLicense("https://www.apache.org/licenses/LICENSE-2.0");
            entity.setSources("https://github.com/Talend/component-runtime");
            entity.setBugtracker("https://jira.talendforge.org/projects/TCOMP/issues");
            entity.setChangelog("Things can change");
            entity.setDocumentation("https://talend.github.io/component-runtime/");
            entity.setVendor(vendor);
            dao.save(entity);
        });
        final ComponentModel found = target.path("component/{id}")
                                             .resolveTemplate("id", entity.getId())
                                             .request(APPLICATION_JSON_TYPE)
                                             .get(ComponentModel.class);
        assertEquals(entity.getId(), found.getId());
        assertEquals(entity.getName(), found.getName());
        assertEquals(entity.getDescription(), found.getDescription());
        context.executeInTx(() -> {
            dao.deleteById(Vendor.class, entity.getVendor().getId());
            dao.deleteById(Component.class, entity.getId());
        });
    }

    @Test
    void deleteById() {
        final Component entity = new Component();
        context.executeInTx(() -> {
            final Vendor vendor = newVendor();
            dao.save(vendor);

            entity.setName("test-component");
            entity.setDescription("Super component");
            entity.setLicense("https://www.apache.org/licenses/LICENSE-2.0");
            entity.setSources("https://github.com/Talend/component-runtime");
            entity.setBugtracker("https://jira.talendforge.org/projects/TCOMP/issues");
            entity.setChangelog("Things can change");
            entity.setDocumentation("https://talend.github.io/component-runtime/");
            entity.setVendor(vendor);
            dao.save(entity);
        });
        final ComponentModel found = context.withToken(token -> target.path("component/{id}")
            .resolveTemplate("id", entity.getId())
            .request(APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, token)
            .delete(ComponentModel.class));
        context.executeInTx(() -> dao.deleteById(Vendor.class, entity.getVendor().getId()));
        context.executeInTx(() -> assertNull(dao.findById(Component.class, entity.getId())));

        assertEquals(entity.getId(), found.getId());
        assertEquals(entity.getName(), found.getName());
        assertEquals(entity.getDescription(), found.getDescription());
    }

    private Component newComponent(final Vendor vendor) {
        final Component entity = new Component();
        entity.setName("test-component");
        entity.setDescription("Super component");
        entity.setLicense("https://www.apache.org/licenses/LICENSE-2.0");
        entity.setSources("https://github.com/Talend/component-runtime");
        entity.setBugtracker("https://jira.talendforge.org/projects/TCOMP/issues");
        entity.setChangelog("Things can change");
        entity.setDocumentation("https://talend.github.io/component-runtime/");
        entity.setVendor(vendor);
        return entity;
    }

    private Vendor newVendor() {
        final Vendor vendor = new Vendor();
        vendor.setName("Talend");
        return vendor;
    }

    private File createCar(final TestInfo info) throws IOException {
        final File car = createTestFile(info);
        car.getParentFile()
           .mkdirs();
        try (final JarOutputStream jar = new JarOutputStream(new FileOutputStream(car))) {
            jar.putNextEntry(new JarEntry("TALEND-INF/metadata.properties"));
            jar.write("version=1.2.3".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return car;
    }

    private File createTestFile(final TestInfo info) {
        return new File(jarLocation(ComponentResourceTest.class).getParentFile(), "test-workdir/ComponentResourceTest/" + info.getTestMethod().get().getName() + "/download.car");
    }
}
