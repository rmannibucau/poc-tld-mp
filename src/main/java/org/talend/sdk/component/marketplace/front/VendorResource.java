package org.talend.sdk.component.marketplace.front;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.marketplace.front.model.VendorModel;
import org.talend.sdk.component.marketplace.model.Vendor;
import org.talend.sdk.component.marketplace.service.mapper.ApplicationMapper;

@Path("vendor")
public class VendorResource extends BaseResource<Vendor, VendorModel> {
    @Inject
    private ApplicationMapper mapper;

    @Override
    protected VendorModel map(final Vendor entity, final boolean withRelationships) {
        return mapper.toModel(entity);
    }

    @Override
    protected void map(final VendorModel model, final Vendor entity) {
        if (model.getName().startsWith("*")) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        entity.setName(model.getName());
    }

    @Override
    protected void beforeFindAll() {
        ensureLogged();
        if (!user.getGroups().contains("**")) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
    }
}
