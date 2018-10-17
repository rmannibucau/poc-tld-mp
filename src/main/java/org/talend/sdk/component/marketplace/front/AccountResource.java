package org.talend.sdk.component.marketplace.front;

import static java.util.Objects.requireNonNull;
import static org.talend.sdk.component.marketplace.model.Account.AccountType.MACHINE;
import static org.talend.sdk.component.marketplace.model.Account.AccountType.SUDO;
import static org.talend.sdk.component.marketplace.model.Account.AccountType.USER;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.marketplace.front.model.AccountModel;
import org.talend.sdk.component.marketplace.front.model.AccountModel;
import org.talend.sdk.component.marketplace.model.Account;
import org.talend.sdk.component.marketplace.model.Vendor;
import org.talend.sdk.component.marketplace.service.mapper.ApplicationMapper;
import org.talend.sdk.component.marketplace.service.security.PasswordHashStrategy;

@Path("account")
public class AccountResource extends BaseResource<Account, AccountModel> {
    @Inject
    private ApplicationMapper mapper;

    @Inject
    private PasswordHashStrategy passwordHashStrategy;

    @Override
    protected AccountModel map(final Account entity, final boolean withRelationships) {
        return mapper.toModel(entity);
    }

    @Override
    protected void map(final AccountModel model, final Account entity) {
        entity.setName(model.getName());
        entity.setLogin(model.getLogin());
        entity.setAccountType(model.isSudo() ? SUDO : (model.isMachine() ? MACHINE : USER));
        if (entity.getId() == null) {
            // todo validate password strength
            model.setPassword(passwordHashStrategy.hash(requireNonNull(model.getPassword())));
        }
        if (model.getVendor() != null) {
            entity.setVendor(entityManager.find(Vendor.class, model.getVendor()));
        }
    }

    @Override
    protected void beforeFindAll() {
        ensureLogged();
        if (!user.getGroups().contains("**")) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
    }
}
