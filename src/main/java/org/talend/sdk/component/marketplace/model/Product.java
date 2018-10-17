package org.talend.sdk.component.marketplace.model;

import java.util.Collection;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.talend.sdk.component.marketplace.front.model.ProductModel;
import org.talend.sdk.component.marketplace.service.ui.View;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString(callSuper = true)
@NamedQueries({
        @NamedQuery(name = "Product.count", query = "select count(c) from Product c"),
        @NamedQuery(name = "Product.findAll", query = "select c from Product c order by c.name")
})
@Table(name = "products")
@View(ProductModel.class)
public class Product extends AuditedEntity {

    @Column(length = 8192, nullable = false)
    private String description;

    @ManyToMany(mappedBy = "products")
    private Collection<Component> components;

    @ElementCollection
    @CollectionTable(name = "product_version", joinColumns = @JoinColumn(name = "product_id"))
    private Collection<String> versions;
}
