package org.talend.sdk.component.marketplace.service.mapper;

import static org.talend.sdk.component.marketplace.model.Account.AccountType.SUDO;

import javax.enterprise.context.ApplicationScoped;

import org.talend.sdk.component.marketplace.front.model.AccountModel;
import org.talend.sdk.component.marketplace.front.model.ComponentModel;
import org.talend.sdk.component.marketplace.front.model.DownloadModel;
import org.talend.sdk.component.marketplace.front.model.ProductModel;
import org.talend.sdk.component.marketplace.front.model.VendorModel;
import org.talend.sdk.component.marketplace.model.Account;
import org.talend.sdk.component.marketplace.model.Component;
import org.talend.sdk.component.marketplace.model.Download;
import org.talend.sdk.component.marketplace.model.Product;
import org.talend.sdk.component.marketplace.model.Vendor;

@ApplicationScoped
public class ApplicationMapper {
    public ComponentModel toModel(final Component entity) {
        final ComponentModel model = new ComponentModel();
        model.setId(entity.getId());
        model.setCreated(entity.getCreated());
        model.setUpdated(entity.getUpdated());
        model.setName(entity.getName());
        model.setDescription(entity.getDescription());
        model.setChangelog(entity.getChangelog());
        model.setSources(entity.getSources());
        model.setLicense(entity.getLicense());
        model.setBugtracker(entity.getBugtracker());
        model.setDocumentation(entity.getDocumentation());
        model.setVendor(entity.getVendor().getId());
        model.setVersion(entity.getVersion());
        return model;
    }

    public ProductModel toModel(final Product entity) {
        final ProductModel model = new ProductModel();
        model.setId(entity.getId());
        model.setCreated(entity.getCreated());
        model.setUpdated(entity.getUpdated());
        model.setName(entity.getName());
        model.setDescription(entity.getDescription());
        model.setVersion(entity.getVersion());
        return model;
    }

    public VendorModel toModel(final Vendor entity) {
        final VendorModel model = new VendorModel();
        model.setId(entity.getId());
        model.setCreated(entity.getCreated());
        model.setUpdated(entity.getUpdated());
        model.setName(entity.getName());
        model.setVersion(entity.getVersion());
        return model;
    }

    public AccountModel toModel(final Account entity) {
        final AccountModel model = new AccountModel();
        model.setId(entity.getId());
        model.setCreated(entity.getCreated());
        model.setUpdated(entity.getUpdated());
        model.setName(entity.getName());
        model.setLogin(entity.getLogin());
        model.setVersion(entity.getVersion());
        model.setVendor(entity.getVendor().getId());
        switch (entity.getAccountType()) {
            case SUDO:
                model.setSudo(true);
                model.setMachine(true);
                break;
            case MACHINE:
                model.setMachine(true);
                break;
            default:
                // no-op
        }
        return model;
    }

    public DownloadModel toModel(final Download entity) {
        final DownloadModel model = new DownloadModel();
        model.setId(entity.getId());
        model.setCreated(entity.getCreated());
        model.setUpdated(entity.getUpdated());
        model.setVersion(entity.getVersion());
        model.setComponentVersion(entity.getComponentVersion());
        return model;
    }
}
