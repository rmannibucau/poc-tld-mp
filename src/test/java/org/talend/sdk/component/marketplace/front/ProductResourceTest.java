package org.talend.sdk.component.marketplace.front;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.marketplace.front.model.ProductModel;
import org.talend.sdk.component.marketplace.front.model.Page;
import org.talend.sdk.component.marketplace.model.Product;
import org.talend.sdk.component.marketplace.service.dao.AuditedEntityDao;
import org.talend.sdk.component.marketplace.service.event.DeletedEvent;
import org.talend.sdk.component.marketplace.test.Context;

@MonoMeecrowaveConfig
class ProductResourceTest {

    @Inject
    private Context context;

    @Inject
    private AuditedEntityDao dao;

    @Inject
    private WebTarget target;

    @Inject
    private Event<DeletedEvent<Product>> deletedEventEvent;

    @Test
    void findAll() {
        context.executeInTx(() -> {
            final Product entity = new Product();
            entity.setName("test-product");
            entity.setDescription("Super product");
            dao.save(entity);
        });
        final Page<ProductModel> products = context.withToken(token -> target.path("product/all").request(APPLICATION_JSON_TYPE)
                                                                             .header(HttpHeaders.AUTHORIZATION, token)
                                                                             .get(ProductModel.Page.class));
        context.executeInTx(() -> dao.deleteById(Product.class, products.getItems().iterator().next().getId()));

        assertEquals(1, products.getItems().size());
        assertEquals(1, products.getTotal());

        final ProductModel model = products.getItems().iterator().next();
        assertNotNull(model.getId());
        assertNotNull(model.getCreated());
        assertNotNull(model.getUpdated());
        assertEquals("test-product", model.getName());
        assertEquals("Super product", model.getDescription());
    }

    @Test
    void save() {
        final ProductModel source = new ProductModel();
        source.setName("save");
        source.setDescription("desc");
        final ProductModel created = context.withToken(token -> target.path("product")
                                             .request(APPLICATION_JSON_TYPE)
                                             .header(HttpHeaders.AUTHORIZATION, token)
                                             .post(entity(source, APPLICATION_JSON_TYPE),
                                                       ProductModel.class));
        assertNotNull(created.getId());
        assertEquals(source.getName(), created.getName());
        assertEquals(source.getDescription(), created.getDescription());
        context.execute(() -> {
            final Product byId = dao.findById(Product.class, created.getId());
            assertNotNull(byId);
            assertEquals(created.getName(), byId.getName());
            assertEquals(created.getDescription(), byId.getDescription());
        });
        context.executeInTx(() -> {
            deletedEventEvent.fire(new DeletedEvent<>(dao.findById(Product.class, created.getId())));
            dao.deleteById(Product.class, created.getId());
        });
    }

    @Test
    void findById() {
        final Product entity = new Product();
        context.executeInTx(() -> {
            entity.setName("test-product");
            entity.setDescription("Super product");
            dao.save(entity);
        });
        final ProductModel found = target.path("product/{id}")
                                             .resolveTemplate("id", entity.getId())
                                             .request(APPLICATION_JSON_TYPE)
                                             .get(ProductModel.class);
        assertEquals(entity.getId(), found.getId());
        assertEquals(entity.getName(), found.getName());
        assertEquals(entity.getDescription(), found.getDescription());
        context.executeInTx(() -> dao.deleteById(Product.class, entity.getId()));
    }

    @Test
    void deleteById() {
        final Product entity = new Product();
        context.executeInTx(() -> {
            entity.setName("test-product");
            entity.setDescription("Super product");
            dao.save(entity);
        });
        final ProductModel found = context.withToken(token -> target.path("product/{id}")
                                             .resolveTemplate("id", entity.getId())
                                             .request(APPLICATION_JSON_TYPE)
                                             .header(HttpHeaders.AUTHORIZATION, token)
                                             .delete(ProductModel.class));
        assertEquals(entity.getId(), found.getId());
        assertEquals(entity.getName(), found.getName());
        assertEquals(entity.getDescription(), found.getDescription());
        context.executeInTx(() -> assertNull(dao.findById(Product.class, entity.getId())));
    }
}
