package org.talend.sdk.component.marketplace.model;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.talend.sdk.component.marketplace.front.model.VendorModel;
import org.talend.sdk.component.marketplace.service.ui.View;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString(callSuper = true)
@NamedQueries({
        @NamedQuery(name = "Vendor.count", query = "select count(c) from Vendor c"),
        @NamedQuery(name = "Vendor.findAll", query = "select c from Vendor c order by c.name")
})
@Table(name = "vendors")
@View(VendorModel.class)
public class Vendor extends AuditedEntity {
}
