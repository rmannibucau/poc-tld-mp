package org.talend.sdk.component.marketplace.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.marketplace.test.Context;
import org.talend.sdk.component.marketplace.model.Product;

@MonoMeecrowaveConfig
class AuditedEntityDaoTest {

    @Inject
    private Context context;

    @Inject
    private AuditedEntityDao dao;

    @Test
    void crud() {
        final Product product = new Product();
        product.setName("crud1");
        product.setDescription("The P1");
        context.executeInTx(() -> dao.save(product));
        context.execute(() -> assertNotNull(dao.findById(Product.class, product.getId())));
        context.executeInTx(() -> dao.deleteById(Product.class, product.getId()));
    }

    @Test
    void count() {
        context.executeInTx(() -> IntStream.range(0, 5).mapToObj(i -> {
            final Product product = new Product();
            product.setName("count" + i);
            product.setDescription("The " + i);
            return product;
        }).forEach(dao::save));
        context.execute(() -> assertEquals(5, dao.count(Product.class)));
        context.executeInTx(() -> dao.findAll(Product.class, 0, 5).forEach(it -> dao.deleteById(Product.class, it.getId())));
    }

    @Test
    void findAllPagination() {
        context.executeInTx(() -> IntStream.range(0, 11).mapToObj(i -> {
            final Product product = new Product();
            product.setName("pagination" + i);
            product.setDescription("The P" + i);
            return product;
        }).forEach(dao::save));
        final Collection<Product> products = new ArrayList<>();
        context.execute(() -> {
            final List<Product> firstPage = dao.findAll(Product.class, 0, 10);
            assertEquals(10, firstPage.size());
            final List<Product> lastPage = dao.findAll(Product.class, 10, 10);
            assertEquals(1, lastPage.size());
            assertTrue(dao.findAll(Product.class, 20, 10).isEmpty());
            products.addAll(firstPage);
            products.addAll(lastPage);
        });
        context.executeInTx(() -> products.forEach(it -> dao.deleteById(Product.class, it.getId())));
    }
}
