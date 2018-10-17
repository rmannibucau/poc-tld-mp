package org.talend.sdk.component.marketplace.front;

import static java.util.Optional.ofNullable;

import java.util.Collections;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.marketplace.front.model.ProductModel;
import org.talend.sdk.component.marketplace.model.Product;
import org.talend.sdk.component.marketplace.service.mapper.ApplicationMapper;

@Path("product")
public class ProductResource extends BaseResource<Product, ProductModel> {
    @Inject
    private ApplicationMapper mapper;

    @Override
    protected ProductModel map(final Product entity, final boolean withRelationships) {
        final ProductModel model = mapper.toModel(entity);
        if (withRelationships) {
            model.setVersions(ofNullable(model.getVersions()).orElseGet(Collections::emptyList));
        }
        return model;
    }

    @Override
    protected void map(final ProductModel model, final Product entity) {
        entity.setName(model.getName());
        entity.setDescription(model.getDescription());
    }

    @Override
    protected void beforeFindAll() {
        ensureLogged();
        if (!user.getGroups().contains("**")) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
    }
}
